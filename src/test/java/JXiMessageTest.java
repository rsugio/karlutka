import karlutka.parsers.pi.XiMessage;
import org.junit.jupiter.api.Test;

public class JXiMessageTest {
    @Test
    void parse() {
        System.out.println("тест русские буквы");

        XiMessage m1 = new XiMessage(KT.Companion.s("/pi_XI/mime1_contentType.txt"),
                KT.Companion.s("/pi_XI/mime1.txt").getBytes());
        System.out.println(m1.getHeader().getMain().getMessageId());
    }
}
