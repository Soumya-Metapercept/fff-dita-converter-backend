<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:mf="http://example.com/mf" expand-text="yes"
    exclude-result-prefixes="#all" version="3.0">

    <xsl:template match="pwc-topic">
<!--        <xsl:text disable-output-escaping="yes">&lt;!DOCTYPE pwc-topic PUBLIC "-//PWC//DTD DITA PWC Topic//EN" "F:\PWC\PWC-Authored-Content-DTD-1.0.6\jcr_root\apps\pwc-madison\dita_resources\com.pwc.doctypes\dtd\pwc-topic.dtd"&gt;</xsl:text>-->
        <pwc-topic>
            <xsl:apply-templates select="@*"/>
            <xsl:apply-templates/>
        </pwc-topic>
    </xsl:template>

    <xsl:param name="format-map" as="map(xs:integer, xs:string)" select="
            map {
                1: 'a',
                2: '1',
                3: 'a'
            }"/>

    <xsl:function name="mf:format" as="xs:string">
        <xsl:param name="number" as="xs:integer"/>
        <xsl:param name="level" as="xs:integer"/>
        <xsl:variable name="formatted-number" select="format-integer($number, $format-map($level))"/>
        <xsl:sequence select="
                if ($level = 1)
                then
                    $formatted-number || '.'
                else
                    '(' || $formatted-number || ')'"/>
    </xsl:function>

    <xsl:function name="mf:group" as="node()*">
        <xsl:param name="items" as="element(li)*"/>
        <xsl:param name="level" as="xs:integer"/>
        <xsl:where-populated>
            <ol>
                <xsl:for-each-group select="$items"
                    group-starting-with="li[@style = 'ListNum' || $level]">
                    <xsl:copy>
                        <xsl:apply-templates
                            select="node() | @* except @style, mf:group(tail(current-group()), $level + 1)"
                        />
                    </xsl:copy>
                </xsl:for-each-group>
            </ol>
        </xsl:where-populated>
    </xsl:function>

    <xsl:mode on-no-match="shallow-copy"/>


    <xsl:template match="LISTING-GROUP">


        <xsl:sequence select="mf:group(li, 1)"/>

    </xsl:template>

</xsl:stylesheet>
