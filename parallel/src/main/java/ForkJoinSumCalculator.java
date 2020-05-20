import java.util.concurrent.RecursiveTask;

public class ForkJoinSumCalculator extends RecursiveTask<Long> {
    private final long[] numbers;
    private final int start;
    private final int end;
    public static final long THRESHOLD = 10_000;

    public ForkJoinSumCalculator(long[] numbers) {
        this(numbers, 0, numbers.length);
    }

    public ForkJoinSumCalculator(long[] numbers, int start, int end) {
        this.numbers = numbers;
        this.start = start;
        this.end = end;
    }

    @Override
    protected Long compute() {
        int length = end - start; // 이 태스크에서 더할 배열의 길이
        if(length <= THRESHOLD) {
            return computeSequentially(); // 기준 값과 같거나 작으면 순차적으로 결과를 계산.
        }

        ForkJoinSumCalculator leftTask = new ForkJoinSumCalculator(numbers, start, start + length / 2);// 작업을 반으로 분할
        leftTask.fork();  //분할된 작업을 반대쪽 스레드가 실행

        ForkJoinSumCalculator rightTask = new ForkJoinSumCalculator(numbers, start + length / 2, end);
        long rightResult = rightTask.compute(); // 현재 스레드가 compute를 다시 재귀호출
        long leftResult = leftTask.join();  // 분할된 작업결과를 기다린다
        return rightResult + leftResult;  // 두 서브태스크의 결과를 조합한 값이 이 태스크의 결과
    }

    // 분할된 배열을 계산
    private long computeSequentially() {
        long sum = 0;
        for (int i = start; i < end; i++) {
            sum += numbers[i];
        }
        return sum;
    }
}


