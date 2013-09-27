package com.gallatinsystems.survey.device.parser;

import java.io.InputStream;
import java.util.List;

public interface FlowParser<E> {
    /**
     * Parse a single element of type <E>
     * @param inputStream the stream containing the element
     * @return parsed value
     */
    public E parse(InputStream inputStream);
    
    /**
     * Parse a single element of type <E>
     * @param response the String response
     * @return parsed value
     */
    public E parse(String response);
    
    /**
     * Parse a list of elements of type <E>
     * @param response the String response
     * @return parsed List of values
     */
    public List<E> parseList(String response);
}
