package com.gallatinsystems.survey.device.parser;

import java.util.List;

public interface FlowParser<E> {
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
