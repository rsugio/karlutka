import java.nio.file.Path;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;

public class JWDAccessLog {
    private final String format;

    static class FileFinder {
        private final String logFileMask;
        private final Path folder;
        private final DateTimeFormatter d, m, y;
        FileFinder(Path folder, String logFileMask) {
            // wd_access_log-%d%m%y
            // icm/HTTP/logging_0	PREFIX=/,LOGFILE=wd_access_log-%d%m%y,SWITCHTF=day
            this.logFileMask = logFileMask;
            this.folder = folder;
            d = DateTimeFormatter.ofPattern("dd");
            m = DateTimeFormatter.ofPattern("MM");
            y = DateTimeFormatter.ofPattern("yyyy");
        }

        String createFileName(ZonedDateTime it) {
            String s = logFileMask;
            s = s.replaceAll("%d", d.format(it));
            s = s.replaceAll("%m", m.format(it));
            s = s.replaceAll("%y", y.format(it));
            return s;
        }

    }

    JWDAccessLog(String format) {
        this.format = format;
    }
    JWDAccessLog() {
        this.format = "%h %l %u %t \"%r\" %s %b";
    }




}
