package com.metapercept.fff_dita_converter.config;

import org.springframework.stereotype.Component;

@Component
public class SharedConfig {
    private String myConfigXmlFilePath;
    private String zipFilePath;

    public String getMyConfigXmlFilePath() {
        return myConfigXmlFilePath;
    }

    public void setMyConfigXmlFilePath(String myConfigXmlFilePath) {
        this.myConfigXmlFilePath = myConfigXmlFilePath;
    }

    public String getZipFilePath() {
        return zipFilePath;
    }

    public void setZipFilePath(String zipFilePath) {
        this.zipFilePath = zipFilePath;
    }
}
