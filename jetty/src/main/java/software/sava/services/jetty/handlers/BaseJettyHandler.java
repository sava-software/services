package software.sava.services.jetty.handlers;

import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

public abstract class BaseJettyHandler extends Handler.Abstract {

  protected static final HttpField JSON_CONTENT = new HttpField(HttpHeader.CONTENT_TYPE, "application/json");

  protected static final HttpField ALLOW_GET = new HttpField(HttpHeader.ACCESS_CONTROL_ALLOW_METHODS, "GET");
  protected static final HttpField ALLOW_POST = new HttpField(HttpHeader.ACCESS_CONTROL_ALLOW_METHODS, "POST");

  protected static String parseParam(final String query, final String param) {
    final int index = query.indexOf(param);
    final int from = index + param.length();
    final int to = query.indexOf('&', from + 1);
    return to < 0
        ? query.substring(from)
        : query.substring(from, to);
  }

  protected final HttpField allowMethod;

  protected BaseJettyHandler(final InvocationType invocationType, final HttpField allowMethod) {
    super(invocationType);
    this.allowMethod = allowMethod;
  }

  protected void setResponseHeaders(final Response response) {
    final var responseHeaders = response.getHeaders();
    responseHeaders.put(allowMethod);
  }

  @Override
  public boolean handle(final Request request, final Response response, final Callback callback) {
    setResponseHeaders(response);
    return false;
  }

  public boolean handle(final HttpFields.Mutable responseHeaders, final Callback callback) {
    responseHeaders.put(allowMethod);
    callback.succeeded();
    return true;
  }
}
