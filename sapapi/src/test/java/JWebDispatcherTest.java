import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;

public class JWebDispatcherTest {
    @Test
    void webdispatcher() {
        ZoneId z = ZoneId.of("Europe/Moscow");
        ZoneId z1 = ZoneId.of("Z");
        ZonedDateTime it = Instant.now().atZone(z);
        System.out.println(it);

        Path f = Paths.get("/workspace/2023-08-15");
        JWDAccessLog.FileFinder ff = new JWDAccessLog.FileFinder(f, "wd_access_log-%d%m%y");
        String s = ff.createFileName(it);
        System.out.println(s);
    }
}
