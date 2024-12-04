package com.nhnacademy.mtqq.Interface;

import com.nhnacademy.mtqq.exception.DataSourceHandlerException;

public interface DataSourceHandler {
    String handle() throws DataSourceHandlerException;
}
