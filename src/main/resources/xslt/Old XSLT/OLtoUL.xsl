<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:mml="http://www.w3.org/1998/Math/MathML" xmlns:m="http://www.w3.org/1998/Math/MathML"
    xmlns:xinfo="http://ns.expertinfo.se/cms/xmlns/1.0" xmlns:xlink="http://www.w3.org/1999/xlink"
    exclude-result-prefixes="xs xsi mml m xinfo xlink" version="2.0">
    
<!--    <xsl:output indent="yes" method="xml" doctype-public="-//PWC//DTD DITA PWC Topic//EN"
        doctype-system="F:\PWC\PWC-Authored-Content-DTD-1.0.6\jcr_root\apps\pwc-madison\dita_resources\com.pwc.doctypes\dtd\pwc-topic.dtd"
        omit-xml-declaration="no" standalone="no"/>-->
    
    <!-- Identity transform to copy all nodes -->
    <xsl:template match="@* | node()">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="ol[li]">
        <xsl:choose>
            <xsl:when test="li[starts-with(normalize-space(.), '•')]">
                <ul>
                    <xsl:apply-templates select="@* except @class | node() "/>
                </ul>
            </xsl:when>
            <xsl:otherwise>
                <ol>
                    <xsl:apply-templates select="@* | node()"/>
                </ol>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
</xsl:stylesheet>
