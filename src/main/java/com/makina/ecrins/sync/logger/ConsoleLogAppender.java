package com.makina.ecrins.sync.logger;

import java.util.Observable;

import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Priority;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.helpers.OnlyOnceErrorHandler;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.OptionHandler;

import com.makina.ecrins.sync.logger.LogMessage.Action;

/**
 * {@link ConsoleLogAppender} notifies all <code>Observer</code>s when appending logs.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class ConsoleLogAppender
        extends Observable
        implements Appender,
                   OptionHandler
{
    private String name;
    private Layout layout;

    /**
     * There is no level threshold filtering by default.
     */
    protected Priority threshold;

    /**
     * It is assumed and enforced that errorHandler is never null.
     */
    protected ErrorHandler errorHandler = new OnlyOnceErrorHandler();

    /**
     * The first filter in the filter chain. Set to <code>null</code> initially.
     */
    private Filter headFilter;

    /**
     * The last filter in the filter chain.
     */
    private Filter tailFilter;

    /**
     * Is this appender closed ?
     */
    protected boolean closed = false;

    public ConsoleLogAppender()
    {

    }

    public ConsoleLogAppender(Layout layout)
    {
        this.layout = layout;
    }

    @Override
    public void addFilter(Filter newFilter)
    {
        if (headFilter == null)
        {
            headFilter = tailFilter = newFilter;
        }
        else
        {
            tailFilter.setNext(newFilter);
            tailFilter = newFilter;
        }
    }

    @Override
    public Filter getFilter()
    {
        return headFilter;
    }

    @Override
    public void clearFilters()
    {
        headFilter = tailFilter = null;
    }

    @Override
    public void close()
    {
        setChanged();
        notifyObservers(
                new LogMessage(
                        Action.RESET,
                        "",
                        Level.OFF
                )
        );
    }

    /**
     * Check whether the message level is below the appender's threshold.
     * If there is no threshold set, then the return value is always <code>true</code>.
     */
    public boolean isAsSevereAsThreshold(Priority priority)
    {
        return ((threshold == null) || priority.isGreaterOrEqual(threshold));
    }

    @Override
    public void doAppend(LoggingEvent event)
    {
        if (closed)
        {
            errorHandler.error("Attempted to append to closed appender named [" + name + "].");
            return;
        }

        if (!isAsSevereAsThreshold(event.getLevel()))
        {
            return;
        }

        if (this.layout == null)
        {
            errorHandler.error("No layout set for the appender named [" + name + "].");
            return;
        }

        Filter f = this.headFilter;

        FILTER_LOOP:
        while (f != null)
        {
            switch (f.decide(event))
            {
                case Filter.DENY:
                    return;
                case Filter.ACCEPT:
                    break FILTER_LOOP;
                case Filter.NEUTRAL:
                    f = f.getNext();
            }
        }

        // notify all registered observers
        setChanged();
        notifyObservers(
                new LogMessage(
                        Action.APPEND,
                        this.layout.format(event),
                        event.getLevel()
                )
        );
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public void setErrorHandler(ErrorHandler errorHandler)
    {
        if (errorHandler == null)
        {
            // We do not throw exception here since the cause is probably a bad config file.
            LogLog.warn("You have tried to set a null error-handler.");
        }
        else
        {
            this.errorHandler = errorHandler;
        }
    }

    @Override
    public ErrorHandler getErrorHandler()
    {
        return this.errorHandler;
    }

    @Override
    public void setLayout(Layout layout)
    {
        this.layout = layout;
    }

    @Override
    public Layout getLayout()
    {
        return layout;
    }

    @Override
    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public boolean requiresLayout()
    {
        return true;
    }

    /**
     * Returns this appenders threshold level.
     * See the {@link #setThreshold} method for the meaning of this option.
     */
    public Priority getThreshold()
    {
        return threshold;
    }

    /**
     * Set the threshold level.
     * All log events with lower level than the threshold level are ignored by the appender.
     * <p/>
     * <p/>
     * In configuration files this option is specified by setting the value of
     * the <b>Threshold</b> option to a level string, such as "DEBUG", "INFO"
     * and so on.
     */
    public void setThreshold(Priority threshold)
    {
        this.threshold = threshold;
    }

    @Override
    public void activateOptions()
    {
        // nothing to do ...
    }
}
