package ness.discovery.client;

import static java.lang.String.format;

import java.io.IOException;

/**
 * If a requested service is not available, this exception is thrown.
 */
public class ServiceNotAvailableException extends IOException
{
    private static final long serialVersionUID = 1L;

    public ServiceNotAvailableException(final String message, final Object ... args)
    {
        super(format(message, args));
    }
}
