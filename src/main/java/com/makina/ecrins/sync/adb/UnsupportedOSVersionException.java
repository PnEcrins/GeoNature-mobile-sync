package com.makina.ecrins.sync.adb;

import org.apache.commons.lang3.SystemUtils;

/**
 * Exception thrown when the current OS is not supported.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class UnsupportedOSVersionException
        extends Exception
{
    private static final long serialVersionUID = -7391624119967100175L;

    public UnsupportedOSVersionException()
    {
        super(SystemUtils.OS_NAME + " (" + SystemUtils.OS_ARCH + ", version : " + SystemUtils.OS_VERSION + ") is not supported");
    }
}
