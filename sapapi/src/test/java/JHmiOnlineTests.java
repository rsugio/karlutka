import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.text.Charsets;
import nl.adaptivity.xmlutil.StAXReader;
import nl.adaptivity.xmlutil.XmlDeclMode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ru.rsug.karlutka.pi.Hmi;
import ru.rsug.karlutka.pi.HmiClient;
import ru.rsug.karlutka.pi.MPI;
import ru.rsug.karlutka.pi.SimpleQuery;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;

@Tag("Online")
public class JHmiOnlineTests {
    private final Map<String, String> po = KT.Companion.props(Paths.get("../.etc/hmi.properties"));
    String authb64 = "Basic " + Base64.getEncoder().encodeToString((po.get("login") + ':' + po.get("passw")).getBytes());
    private final URI uriRep = URI.create(po.get("url") + "/rep/query/int?container=any");
    private final URI uriDir = URI.create(po.get("url") + "/dir/query/int?container=any");
    private final HmiClient hmiclient = HmiClient.Companion.simpleQueryClient("7.0", "anyuser");
    private final HttpClient httpclient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(100))
//        .authenticator(po["auth"] as Authenticator)  // не работает в случае форм, так как выдаётся rc=200
            .build();

    private SimpleQuery.QueryResult doSimpleQuery(URI uri, SimpleQuery.QueryRequest req) throws Exception {
        String xml = hmiclient.request("QUERY_REQUEST_XML", req.encodeToString()).toInstance().encodeToString(XmlDeclMode.None);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", authb64)
                .header("Content-Type", "application/xml; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(xml))
                .build();

        HttpResponse<InputStream> response = this.httpclient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() == 200) {
            Hmi.Instance instance = Hmi.Companion.decodeInstanceFromReader(new StAXReader(response.body(), "UTF-8"));
            Hmi.HmiResponse hmiresponse = this.hmiclient.parseResponse(instance);
            if (hmiresponse.getMethodOutputReturn() != null) {
                return SimpleQuery.Companion.decodeQueryResultFromString(hmiresponse.getMethodOutputReturn());
            } else {
                Object var14 = hmiresponse.getCoreException();
                if (var14 == null) {
                    var14 = hmiresponse.getMethodFault();
                }
                if (var14 == null) {
                    var14 = "Unknown error";
                }
                throw new IllegalStateException(var14.toString());
            }
        } else {
            System.err.println(response);
            String var15 = new String(response.body().readAllBytes(), Charsets.UTF_8);
            throw new IllegalStateException(var15);
        }
    }

    @Test
    public final void dirAllList() throws Exception {
        SimpleQuery.QueryRequest allDir75 = SimpleQuery.Companion.queryRequestDir(
                MPI.Companion.getDir75alltypes(),
                CollectionsKt.listOf(SimpleQuery.EResult.RA_XILINK, SimpleQuery.EResult.OBJECTID, SimpleQuery.EResult.TEXT),
                new SimpleQuery.Condition()
        );
        SimpleQuery.QueryResult resp = this.doSimpleQuery(uriDir, allDir75);
        for (SimpleQuery.R r : resp.getMatrix().getR()) {
            SimpleQuery.Qref qref = (r.getC().get(0)).getQref();
            assert qref != null;
            MPI.ETypeID typeID = qref.getRef().getKey().getTypeID();
            String oid = Objects.requireNonNull((r.getC().get(1)).getSimple()).getBin();
            String desc = Objects.requireNonNull((r.getC().get(2)).getSimple()).getStrg();
            String var12 = typeID.toString() + '\t' + oid + " = " + desc;
            System.out.println(var12);
        }

    }

}
