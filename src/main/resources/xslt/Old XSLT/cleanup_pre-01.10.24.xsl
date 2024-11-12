<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xd="http://www.oxygenxml.com/ns/doc/xsl"
    exclude-result-prefixes="xs xd" version="2.0">


    <xsl:output indent="yes" method="xml" doctype-public="-//PWC//DTD DITA PWC Topic//EN"
        doctype-system="pwc-topic.dtd" omit-xml-declaration="no" standalone="no"/>

    <!-- Identity transform to copy all nodes -->
    <xsl:template match="@* | node()">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="p[parent::b] | p[parent::i]">
        <xsl:apply-templates/>
    </xsl:template>

    <xsl:template match="b[p]">
        <p>
            <b>
                <xsl:apply-templates/>
            </b>
        </p>
    </xsl:template>

    <xsl:template match="i[p]">
        <p>
            <i>
                <xsl:apply-templates/>
            </i>
        </p>
    </xsl:template>

    <xsl:template match="pwc-xref[parent::title]">
        <ph>
            <pwc-xref>
                <xsl:copy-of select="@*"/>
                <xsl:apply-templates/>
            </pwc-xref>
        </ph>
    </xsl:template>

    <!--    <xsl:template match="entry/@align[contains(., '_dd') or contains(., 'Table_bullet')]"/>-->

    <xsl:template match="entry">
        <entry>
            <xsl:apply-templates select="@* except @align"/>
            <xsl:choose>
                <xsl:when test="@align = 'Table_Right_Header'">
                    <xsl:attribute name="align">
                        <xsl:value-of select="'right'"/>
                    </xsl:attribute>
                </xsl:when>
                <xsl:when test="@align = 'Centre'">
                    <xsl:attribute name="align">
                        <xsl:value-of select="'center'"/>
                    </xsl:attribute>
                </xsl:when>
                <!-- added on 19-09-2024 -->
                <xsl:when test="@align = 'table_-_ind_x_2'">
                    <xsl:attribute name="align">
                        <xsl:value-of select="'left'"/>
                    </xsl:attribute>
                </xsl:when>
                <xsl:when test="@align = 'Paragraph'"/>
                <xsl:when test="contains(@align, '_dd')"/>
                <xsl:when test="contains(@align, 'Table_bullet')"/>
                <xsl:when test="contains(@align, 'BulletNoIndent')"/>
                <xsl:when test="contains(@align, 'Paragraph')"/>
                <xsl:when test="contains(@align, 'bullet')"/>
                <xsl:when test="contains(@align, 'Numbered_paragraph')"/>
                <xsl:when test="contains(@align, 'L5CAS')"/>
                <xsl:otherwise>
                    <xsl:if test="@align">
                        <xsl:attribute name="align">
                            <xsl:value-of select="@align"/>
                        </xsl:attribute>
                    </xsl:if>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:apply-templates/>
        </entry>
    </xsl:template>

</xsl:stylesheet>
