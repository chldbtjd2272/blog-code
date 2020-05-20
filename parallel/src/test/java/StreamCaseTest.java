import org.junit.Test;
import org.junit.rules.Stopwatch;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class StreamCaseTest {
    private static final long ANSWER = 8000000002000000000L;

    public long sum(long limit) {
        long sum = 0;
        for (long i = 1; i <= limit; i++) {
            sum += i;
        }

        return sum;
    }

    @Test
    public void name() {
        //given

        //when
        assertThat(sum(4000000000L)).isEqualTo(ANSWER);
        //then
    }

    public long sumStream(long limit) {
        return LongStream.rangeClosed(1, limit)
                .reduce(0L, Long::sum);
    }

    @Test
    public void name2() {
        //given
        //when
        assertThat(sumStream(4000000000L)).isEqualTo(ANSWER);
        //then
    }

    public long sumParallelStream(long limit) {
        long startTime = System.currentTimeMillis();
        long result = LongStream.rangeClosed(1, limit)
                .parallel()
                .reduce(0L, Long::sum);
        System.out.println(System.currentTimeMillis() - startTime);
        return result;
    }

    @Test
    public void name3() {
        //given
        //when
        assertThat(sumParallelStream(4000000000L)).isEqualTo(ANSWER);
        //then
    }

    @Test
    public void name4() {
        //given
        //when
        long[] numbers = LongStream.rangeClosed(1, 400000L).toArray();
        ForkJoinTask<Long> task  = new ForkJoinSumCalculator(numbers);

        //then
        assertThat(new ForkJoinPool().invoke(task)).isEqualTo(80000200000L);
    }
}