import item10.SynchronizedPoint;
import org.junit.Test;

public class ThreadTest {

    @Test
    public void name() throws InterruptedException {
        //given
        //when
        SynchronizedPoint synchronizedPoint = new SynchronizedPoint();
        synchronizedPoint.start(null);

        Thread.sleep(1000L);

        synchronizedPoint.stop();
    }
}
