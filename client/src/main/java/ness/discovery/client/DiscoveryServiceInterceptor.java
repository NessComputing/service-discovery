package ness.discovery.client;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.nesscomputing.httpclient.HttpClientObserver;
import com.nesscomputing.httpclient.HttpClientRequest;
import com.nesscomputing.httpclient.internal.HttpClientHeader;
import com.nesscomputing.logging.Log;

/**
 * Translates "srvc://" URIs into service discovered http/https URIs.
 *
 * Service discovery hints can be added as headers prefixed with "X-Ness-SDHint-"
 */
public class DiscoveryServiceInterceptor extends HttpClientObserver
{
    private static final Log LOG = Log.findLog();

    private final ServiceURIConverter serviceUriConverter;

    @Inject
    DiscoveryServiceInterceptor(final ServiceURIConverter serviceUriConverter)
    {
        this.serviceUriConverter = serviceUriConverter;
    }

    @Override
    public <RequestType> HttpClientRequest<RequestType> onRequestSubmitted(final HttpClientRequest<RequestType> request)
        throws IOException
    {
        final URI requestUri = request.getUri();
        if (!"srvc".equals(requestUri.getScheme())) {
            return request;
        }
        else {
            try {
                final ServiceURI serviceURI = new ServiceURI(request.getUri());
                LOG.trace("Found service URI: %s", serviceURI);
                List<ServiceHint> hints = Lists.newArrayList();
                for (HttpClientHeader header: request.getHeaders()) {
                	String name = header.getName();
                	//Extract any service discovery hints
					if (name.startsWith("X-Ness-SDHint-") && name.length() > "X-Ness-SDHint-".length()) {
                		hints.add(new ServiceHint(name.substring("X-Ness-SDHint-".length(), name.length()), header.getValue()));
                	}
                }
                final URI newUri = serviceUriConverter.convertServiceURI(serviceURI, hints.toArray(new ServiceHint[hints.size()]));
                final HttpClientRequest.Builder<RequestType> builder = HttpClientRequest.Builder.fromRequest(request);
                LOG.trace("New URI now %s", newUri);
                builder.setUrl(newUri);
                return builder.request();
            }
            catch (URISyntaxException use) {
                throw new IOException("Malformed service URI", use);
            }
        }
    }
}
