import java.util.stream.LongStream;

public class StreamCase {

    public long sum(long limit) {
        long sum = 0;
        for (long i = 0; i < limit; i++) {
            sum += i;
        }
        return sum;
    }


    public long sumStream(long limit){
        return LongStream.rangeClosed(1,limit)
                .reduce(0L,Long::sum);
    }


    public long sumParallelStream(long limit){
        return LongStream.rangeClosed(1,limit)
                .parallel()
                .reduce(0L,Long::sum);
    }
}
