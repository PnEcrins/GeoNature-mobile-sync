package com.makina.ecrins.sync.tasks;

/**
 * Exception thrown by {@link AbstractTaskRunnable}.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class TaskException
        extends Exception
{
    private static final long serialVersionUID = -4875432714583248671L;

    public TaskException(String message)
    {
        super(message);
    }

    public TaskException(String message,
                         Throwable cause)
    {
        super(
                message,
                cause
        );
    }
}
