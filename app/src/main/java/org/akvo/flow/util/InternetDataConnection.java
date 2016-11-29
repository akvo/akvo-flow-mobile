package org.akvo.flow.util;

import org.akvo.flow.exception.HttpException;
import org.akvo.flow.util.FileUtil;
import org.akvo.flow.util.HttpUtil;
import org.apache.http.HttpStatus;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * InternetDataConnection represents a connection to a URL, it supports both Input/Output. <br>
 * After creating this class, you need to activate .connect() and decorate it with for-methods (Input/Output)
 */
public class InternetDataConnection implements Closeable
{
    private long startTime;
    private URL url;
    private HttpURLConnection connection;

    private boolean connected = false;

    private InternetDataConnection.InputStreamProvider input;
    private InternetDataConnection.OutputStreamProvider output;

    /**
     * creates a new InternetDataConnection to the chosen URL, may throw MalformedURLException if the URL looks broken
     * @param url
     * @throws MalformedURLException
     */
    public InternetDataConnection(String url) throws MalformedURLException
    {
        this(new URL(url));
    }

    /**
     * creates a new InternetDataConnection towards the chosen URL
     * @param url
     */
    public InternetDataConnection(URL url)
    {
        this.url = url;
    }



    /**
     * (Mainly for testing purposes, injecting dependency)
     * makes the InternetDataConnection use the specified HttpURLConnection object
     * @param connection
     */
    public InternetDataConnection(HttpURLConnection connection)
    {
        this.connection = connection;
    }

    /**
     * tells InternetDataConnection to actually start connecting. <br>
     * This method returns itself and allows you to decorate with what you wanna use the connection for.
     * You can choose between:
     * <ul>
     *
     * <li> Input only, use forInput() </li>
     * <li> Output only, use forOutput() </li>
     * <li> Both, use any of the above, and then andOutput()/andInput() </li>
     * </ul>
     *
     * example:
     * .connect().forInput().andOutput();
     * @return this
     * @throws IOException
     */
    public InternetDataConnection connect() throws IOException
    {
        if (connection == null)
        {
            connection = (HttpURLConnection) url.openConnection();
            startTime = System.currentTimeMillis();
            connection.setDoInput(false);
            connection.setDoOutput(false);
        }
        return this;
    }

    /**
     * {@link HttpURLConnection#getResponseMessage()}
     */
    public String getResponseMessage() throws IOException
    {
        return connection.getResponseMessage();
    }

    /**
     * returns the time between now and from the point that .connect() was called in milliseconds
     * @return int delta time between now and .connect() in millis
     */
    public int getElapsedTime()
    {
        return (int) (System.currentTimeMillis() - startTime);
    }

    /**
     * {@link HttpURLConnection#getResponseCode()}
     */
    public int getStatusCode() throws IOException
    {
        try
        {
            return connection.getResponseCode();
        } catch (IOException e)
        {
            // HttpUrlConnection will throw an IOException if any 4XX
            // response is sent. If we request the status again, this
            // time the internal status will be properly set, and we'll be
            // able to retrieve it.
            return connection.getResponseCode();
        }
    }

    /**
     * checks that the statusCode is 200 (OK)
     * throws an exception if its not
     * @throws IOException
     */
    public void verifyOk() throws IOException
    {
        if (!isOk()) {
            throw createHttpException();
        }
    }

    /**
     * return true if the statusCode is 200 (OK)
     * @throws IOException
     */
    public boolean isOk() throws IOException
    {
        int status = getStatusCode();
        return (status == HttpStatus.SC_OK);
    }

    /**
     * creates a new instance of a HttpException object with the responseMessage and the statusCode
     * @return a new HttpException object for throwing
     * @throws IOException
     */
    public HttpException createHttpException() throws IOException
    {
        if (isOk())
        {
            throw new RuntimeException("Trying to create HttpException for working connection");
        }
        return new HttpException(getResponseMessage(), getStatusCode());
    }

    /**
     * tells the object to disconnect the connection and close all related streams
     * @throws IOException
     */
    @Override
    public void close() throws IOException
    {
        if (connection != null)
        {
            if (input != null)
            {
                FileUtil.close(input.get()); // streams are singleton's from URL connection impl so doing this works
            }
            if (output != null)
            {
                FileUtil.close(output.get());
            }
            connection.disconnect();
        }
    }

    /**
     * enables InternetDataConnection to only use input stream. <br>
     * If you want to use both Input and Output, decorate this method with andOutput();
     *
     * example:
     * {@code .connect().forInput().andOutput();}
     *
     * @return an object that provides you with a BufferedInputStream only
     */
    public InternetDataConnection.InputStreamProvider forInput()
    {
        if (input != null)
            throw new RuntimeException("Input has already been enabled");
        if (connected)
            throw new RuntimeException("Cannot enable input after having already connected");
        connection.setDoInput(true);
        input = new InternetDataConnection.InputStreamProvider();
        return input;
    }

    /**
     * class giving you access to a BufferedInputStream
     * and related methods such as writing it out to a string or copying it to an outputStream
     */
    public class InputStreamProvider
    {
        /**
         * @return the BufferedInputStream associated with the connection
         * @throws IOException
         */
        public BufferedInputStream get() throws IOException
        {
            connected = true;
            return new BufferedInputStream(connection.getInputStream());
        }

        /**
         * changes this InputStreamProvider given by forInput() into a BothStreams that gives you
         * access to both InputStreamProvider and OutputStreamProvider
         *
         * @return BothStreams with {@code this} and {@link InternetDataConnection.OutputStreamProvider}
         */
        public InternetDataConnection.BothStreams andOutput()
        {
            if (connected)
                throw new RuntimeException("Cannot enable output after having already connected");
            return new InternetDataConnection.BothStreams(this, new InternetDataConnection.OutputStreamProvider());
        }

        /**
         * writes the InputStream to a string
         * @return the string created from reading the InputStream
         * @throws IOException
         */
        public String toStringValue() throws IOException
        {
            BufferedInputStream inputStream = get();
            StringBuilder builder = new StringBuilder();

            int value;
            while ((value = inputStream.read()) != -1)
            {
                builder.append((char) value);
            }
            builder.append('\n');
            return builder.toString();
        }

        /**
         * copies the data in this InputStream into the given OutputStream, using {@link HttpUtil#copyStream}
         * @param outputStream destination stream
         * @throws IOException
         */
        public void toStream(OutputStream outputStream) throws IOException
        {
            HttpUtil.copyStream(get(), outputStream);
        }
    }

    /**
     * enables InternetDataConnection to only use output stream
     * if you want to use both Input and Output, decorate this method with andInput();
     *
     * like this:
     * {@code .connect().forOutput().andInput();}
     *
     * @return an object that provides you with a BufferedOutputStream only
     */
    public InternetDataConnection.OutputStreamProvider forOutput()
    {
        if (output != null)
            throw new RuntimeException("Output has already been enabled");
        if (connected)
            throw new RuntimeException("Cannot enable output after having already connected");
        connection.setDoOutput(true);
        output = new InternetDataConnection.OutputStreamProvider();
        return output;
    }

    /**
     * class giving you access to a BufferedOutputStream
     */
    public class OutputStreamProvider
    {
        /**
         * @return the BufferedOutputStream associated with the connection
         * @throws IOException
         */
        public BufferedOutputStream get() throws IOException
        {
            connected = true;
            return new BufferedOutputStream(connection.getOutputStream());
        }

        /**
         * changes this OutputStreamProvider given by forOutput() into a BothStreams that gives you
         * access to both InputStreamProvider and OutputStreamProvider
         *
         * @return BothStreams with this and InputStreamProvider
         */
        public InternetDataConnection.BothStreams andInput()
        {
            if (connected)
                throw new RuntimeException("Cannot enable output after having already connected");
            return new InternetDataConnection.BothStreams(new InternetDataConnection.InputStreamProvider(), this);
        }
    }

    /**
     * a class containing both the InputStreamProvider and OutputStreamProvider
     * accessed through public final fields
     */
    public class BothStreams
    {
        public final InternetDataConnection.InputStreamProvider input;
        public final InternetDataConnection.OutputStreamProvider output;

        /**
         * creates a new BothStreams object with public access to both fields
         * @param input
         * @param output
         */
        public BothStreams(InternetDataConnection.InputStreamProvider input, InternetDataConnection.OutputStreamProvider output)
        {
            this.input = input;
            this.output = output;
        }
    }
}