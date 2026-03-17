package com.compiler.model;

public class CodeRequest {
    private String language;
    private String sourceCode;
    private String inputData;

    public CodeRequest() {}
    public String getLanguage()           { return language; }
    public void   setLanguage(String v)   { this.language = v; }
    public String getSourceCode()         { return sourceCode; }
    public void   setSourceCode(String v) { this.sourceCode = v; }
    public String getInputData()          { return inputData; }
    public void   setInputData(String v)  { this.inputData = v; }
}
