<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema" exclude-result-prefixes="xs" version="2.0">

    <xsl:template match="@* | node()">
        <xsl:copy copy-namespaces="no">
            <xsl:apply-templates select="@* except @class | node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template
        match="li[@outputclass = 'bullet' or @outputclass = 'IFRSBoldBullet' or @outputclass = 'Table_bullet'] | li[@outputclass = 'sub-bullet' or @outputclass = 'IFRSBoldSub-bullet' or @outputclass = 'table_sub_bullet']">

        <xsl:if test="not(preceding-sibling::*[1][self::li])">
            <xsl:text disable-output-escaping="yes"><![CDATA[<LISTING-GROUP>]]></xsl:text>
        </xsl:if>
        <xsl:copy>
            <xsl:apply-templates select="@*"/>
            <xsl:apply-templates/>
        </xsl:copy>
        <xsl:if test="not(following-sibling::*[1][self::li])">
            <xsl:text disable-output-escaping="yes"><![CDATA[</LISTING-GROUP>]]></xsl:text>
        </xsl:if>

    </xsl:template>


</xsl:stylesheet>
