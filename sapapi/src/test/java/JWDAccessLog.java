import java.nio.file.Path;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

public class JWDAccessLog {
    private final String format;

    static class FileFinder {
        private final String logFileMask;
        private final Path folder;
        private final DateTimeFormatter d, m, y, h, t, s;
        FileFinder(Path folder, String logFileMask) {
            // wd_access_log-%d%m%y
            // icm/HTTP/logging_0	PREFIX=/,LOGFILE=wd_access_log-%d%m%y,SWITCHTF=day
            this.logFileMask = logFileMask;
            this.folder = folder;
            d = DateTimeFormatter.ofPattern("dd");
            m = DateTimeFormatter.ofPattern("MM");
            y = DateTimeFormatter.ofPattern("yyyy");
            h = DateTimeFormatter.ofPattern("HH");
            t = DateTimeFormatter.ofPattern("mm");  //t ~> mm
            s = DateTimeFormatter.ofPattern("ss");
        }

        String createFileName(ZonedDateTime it) {
            String x = logFileMask;
            //https://help.sap.com/doc/saphelp_nw74/7.4.16/en-us/48/8fe37933114e6fe10000000a421937/frameset.htm
            x = x.replaceAll("%d", d.format(it));
            x = x.replaceAll("%m", m.format(it));
            x = x.replaceAll("%y", y.format(it));
            x = x.replaceAll("%h", h.format(it));
            x = x.replaceAll("%t", t.format(it));
            x = x.replaceAll("%s", s.format(it));
            x = x.replaceAll("%%", "%");
            return x;
        }
    }

    private final Pattern patLine;
    JWDAccessLog(String format) {
        this.format = format;
        List<String> parts = new ArrayList<>();
        parts.add("%b");
        parts.add("%r");
        parts.add("%r0");
        parts.add("%r1");
        parts.add("%r2");
        parts.add("%f");
        parts.add("%U");
        parts.add("%s");
        parts.add("%p0");
        parts.add("%p1");
        parts.add("%p2");
        parts.add("%p3");
        parts.add("%{name}i");
        parts.add("%{[?]}i");
        parts.add("%{?;}i");
        parts.add("%{name}o");
        parts.add("%{[?]}o");
        parts.add("%{?;}o");
        parts.add("%{cookie}c");
        parts.add("%{cookie}C");
        parts.add("%{formfield}F");
        parts.add("%{semfield}E");
        parts.add("%T");
        parts.add("%L");
        parts.add("%j");
        parts.add("%h");
        parts.add("%h0");
        parts.add("%h1");
        parts.add("%H");
        parts.add("%V");
        parts.add("%v");
        parts.add("%a");
        parts.add("%S");
        parts.add("%l");
        parts.add("%u");
        parts.add("%t");
        parts.add("%q");
        parts.add("%M1");
        parts.add("%M2");
        parts.add("%m1");
        parts.add("%m2");
        parts.add("%B1");
        parts.add("%B2");
        parts.add("%w1");
        parts.add("%w2");


        int i=0;
        String pat = "";
        char[] fs = format.toCharArray();
        while (i<fs.length) {
            char c = fs[i];
            i++;
            if (c=='%' && i<fs.length) {

                i++;

            }
        }
        System.out.println(pat);
        patLine = Pattern.compile(pat);
    }
    JWDAccessLog() {
        this("%h %l %u %t \"%r\" %s %b");
    }
    class LogLine {
        // https://help.sap.com/doc/saphelp_nw74/7.4.16/en-us/48/8fe37933114e6fe10000000a421937/frameset.htm
        String h_ost;   // Name of the remote host (the client, such as the browser)
        String l_;  //Specifies the “remote log name”. This name is the result of an IDENT query to the client. This only works if the identity check is activated there.
        String u_ser;   //User name of 401 authentication
        String t_ime; // Time specification in CLF format: [15/Dec/2007:16:18:35 +0100]
        String r_esource;   //Line of an HTTP request with the original path and form fields
        int s_tatus;    //OK code of the response.
        long b_ytes;    //Length of the response in bytes
        LogLine(String s, Pattern pattern) {


        }
    }


}
