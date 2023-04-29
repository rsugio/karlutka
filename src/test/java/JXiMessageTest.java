import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;
import karlutka.parsers.pi.XiMessage;
import org.junit.jupiter.api.Test;

public class JXiMessageTest {
    TimeBasedGenerator UUIDgenerator = Generators.timeBasedGenerator();

    String nextuuid() {
        return UUIDgenerator.generate().toString();
    }

    @Test
    void parse() {
        System.out.println("тест русские буквы и разбор");

        XiMessage m1 = new XiMessage(KT.Companion.s("/pi_XI/mime1_contentType.txt"),
                KT.Companion.s("/pi_XI/mime1.txt").getBytes());
        System.out.println(m1.getHeader().getMain().getMessageId());
    }

    @Test
    void create() {
        XiMessage.Main m1 = new XiMessage.Main(XiMessage.MessageClass.ApplicationMessage, XiMessage.ProcessingMode.asynchronous,
                nextuuid(), null, XiMessage.Companion.dateTimeSentNow(),
                new XiMessage.PartyService("P_PARTY", "D0001"),
                null,
                new XiMessage.Interface("urn:demo", "dummy-interface")
        );
        XiMessage.Header h1 = new XiMessage.Header(m1, new XiMessage.ReliableMessaging(null));
        XiMessage xi1 = new XiMessage(h1);
        xi1.writeTo(System.out);
    }
}
