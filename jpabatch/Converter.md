### Converter

> jpa converter를 사용하면 db에 저장할때 entity로 로드할때 필드값을 자동으로 컨버팅할 수 있는 기술이다.
>
> 해당 기술을 잘못 사용할 경우 발생하는 문제를 정리하는 예제이다.



```java
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = ProfileConverter.class)
    private Profile profile;

    public Person(Profile profile) {
        this.profile = profile;
    }
}

@Getter
@EqualsAndHashCode
@NoArgsConstructor
public class Profile {
    private String name;
    private String image;
    private String path;

    public Profile(String name, String image) {
        this.name = name;
        this.image = image;
    }

    public void setPath(String path){
        this.path = path;
    }
}


@Converter
public class ProfileConverter implements AttributeConverter<Profile, String> {
    private static final String PATH = "/image";
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Profile attribute) {
        try {
            return mapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new RuntimeException();
        }
    }

    @Override
    public Profile convertToEntityAttribute(String dbData) {
        try {
            Profile profile = mapper.readValue(dbData, Profile.class);
            profile.setPath(PATH);
            return profile;
        } catch (JsonProcessingException e) {
            throw new RuntimeException();
        }
    }
}
```

- 위 예제는 Person을 저장할때 profile을 json 형태의 String으로 변경해서 저장하고 로드할땐 다시 객체형태로 변경한다.
- 여기서 문제는 convertToEntityAttribute를 호출할때 객체에서 사용할 수 있도록 path값을 추가한다는 점이다.
  - image path는 db에 저장되는 값이 아닌 객체를 로드할때 마다 매번 변경될 수 있는 값으로 판단하고 코드에서 동적으로 주입한다.
  - 간단한 예시를 위해 하드코딩
- db에 저장할때 convertToDatabaseColumn를 호출해서 저장될거라 판단했지만, 실제는 저장되기 전에
  convertToDatabaseColumn->convertToEntityAttribute->convertToDatabaseColumn 순으로 호출되어 저장된다.
-  하이버네이트는 더티체킹을 위해 db에 원본 데이터를 저장하고 복사본을 만들어 메모리에 관리한다. 이 과정에서 원본 객체를 deepCopy하는데 
  convertToDatabaseColumn를 호출해 컨버팅 클래스를 string으로 변환하고 해당 데이터를 다시 String으로 컨버팅해 db에 저장한다.

```java
public class AttributeConverterMutabilityPlanImpl<T> extends MutableMutabilityPlan<T> {
	private final JpaAttributeConverter converter;

	public AttributeConverterMutabilityPlanImpl(JpaAttributeConverter converter) {
		this.converter = converter;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected T deepCopyNotNull(T value) {
		return (T) converter.toDomainValue( converter.toRelationalValue( value ) );
	}
}

```

- 위 과정을 수행하게 되면 path가 추가된 데이터가 db에 insert된다
- 캐싱 데이터에는 path가 추가된 데이터가 저장되어 있고, 원본 객체에는 path가 없는 데이터가 존재한다.

```java
@Transactional
	@Override
	public <S extends T> S save(S entity) { 

		Assert.notNull(entity, "Entity must not be null.");

		if (entityInformation.isNew(entity)) {
			em.persist(entity);
			return entity;-----> entity객체에는 path가 없고, 실제 db와 캐시데이터에는 path필드가 존재한다.
		} else {
			return em.merge(entity);
		}
	}

```

- JpaRepository의 save 메서드는 Transactional로 묶여있고, commit을 찍기전 원본데이터 (path x)와 캐시 데이터(path o)가 있는 데이터를 더티체킹하고 변경되었다고 판단하여 update쿼리를 수행한다. 
  - Path가 빠진 Profile데이터로 update된다. 원래 의도한대로 db에 값은 유지되지만, 단순 insert만 수행되어야하는 상황에 update까지 같이 수행하게 된다.
- 위와 같은 문제를 방지하려면 convert를 수행할 때 필드의 값을 추가하거나 제거하면 안된다. 