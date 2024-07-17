<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:mml="http://www.w3.org/1998/Math/MathML" xmlns:m="http://www.w3.org/1998/Math/MathML"
    xmlns:xinfo="http://ns.expertinfo.se/cms/xmlns/1.0" xmlns:xlink="http://www.w3.org/1999/xlink"
    xmlns:ditaarch="http://dita.oasis-open.org/architecture/2005/"
    xmlns:xd="http://www.oxygenxml.com/ns/doc/xsl"
    exclude-result-prefixes="xs xsi mml m xinfo xlink xd ditaarch" version="2.0">
    
    <xsl:output indent="yes"/>
    
    <xsl:template match="node() | @*">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="file">
<!--        <xsl:text disable-output-escaping="yes">&lt;!DOCTYPE pwc-topic PUBLIC "-//PWC//DTD DITA PWC Topic//EN" "F:\PWC\PWC-Authored-Content-DTD-1.0.6\jcr_root\apps\pwc-madison\dita_resources\com.pwc.doctypes\dtd\pwc-topic.dtd"&gt;</xsl:text>-->
        <pwc-topic>
            
            <xsl:choose>
                <xsl:when test="body/div[1]/p/span/a/@id">
                    <xsl:attribute name="id" select="body/div[1]/p/span/a/@id"/>
                </xsl:when>
                <xsl:when test="body/div[1]/p/a/@id">
                    <xsl:attribute name="id" select="body/div[1]/p/a/@id"/>
                </xsl:when>
                <xsl:when test="body/div[1]/a[@id and @name]">
                    <xsl:attribute name="id">
                        <xsl:value-of select="body/div[1]/a[@name]/@id"/>
                    </xsl:attribute>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:if test="@uri_fragment">
                        <xsl:attribute name="id">
                            <xsl:value-of select="substring-after(@uri_fragment, '#')"/>
                        </xsl:attribute>
                    </xsl:if>
                </xsl:otherwise>
            </xsl:choose>
            
            <xsl:if test="body/div[1]/p/node()">
                <title>
                    <xsl:if test="body/div[1]/@id">
                        <xsl:attribute name="id">
                            <xsl:value-of select="body/div[1]/@id"/>
                        </xsl:attribute>
                    </xsl:if>
                    <xsl:apply-templates select="body/div[1]/p/node()"/>
                </title>
            </xsl:if>
            <xsl:apply-templates/>
        </pwc-topic>
    </xsl:template>
    
    <xsl:template match="body">
        <pwc-body>
            <section>
                <xsl:if test="./div[2]/p/node()">
                    <title>
                        <xsl:if test="../body/div[2]/@id">
                            <xsl:attribute name="id">
                                <xsl:value-of select="../body/div[2]/@id"/>
                            </xsl:attribute>
                        </xsl:if>
                        <xsl:apply-templates select="./div[2]/p/node()"/>
                    </title>
                </xsl:if>
                <xsl:apply-templates select="node() except (./div[1],./div[2])"/>
            </section>
        </pwc-body>
    </xsl:template>
    
    <xsl:template match="div">
         <xsl:apply-templates/>
    </xsl:template>
    
    <xsl:template match="p">
        <p>
            <xsl:choose>
                <xsl:when test="span/a/@id">
                    <xsl:attribute name="id" select="span/a/@id"/>
                </xsl:when>
                <xsl:when test="a/@id">
                    <xsl:attribute name="id" select="a/@id"/>
                </xsl:when>
            </xsl:choose>
            <xsl:apply-templates/>
        </p>
    </xsl:template>
    
    <xsl:template name="countCommas">
        <xsl:param name="string" />
        <xsl:param name="count" />
        
        <xsl:choose>
            <xsl:when test="contains($string, ',')">
                <xsl:variable name="remaining" select="substring-after($string, ',')" />
                <xsl:call-template name="countCommas">
                    <xsl:with-param name="string" select="$remaining" />
                    <xsl:with-param name="count" select="$count + 1" />
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <!-- Output the final count -->
                <xsl:value-of select="$count" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="table">
        <xsl:variable name="colWidths" select="tokenize(@data-colWidths, ',')"/>
        <table>
            <tgroup>
                <xsl:attribute name="cols">
                    <xsl:variable name="cols1" select="@data-colWidths" />
                    <xsl:call-template name="countCommas">
                        <xsl:with-param name="string" select="$cols1" />
                        <xsl:with-param name="count" select="1" />
                    </xsl:call-template>
                </xsl:attribute>
                
                <xsl:choose>
                    <xsl:when test="tr[1]/th">
                        <xsl:for-each select="$colWidths">
                            <colspec colname="col{position()}"/>
                        </xsl:for-each>
                    </xsl:when>
                    <xsl:when test="tr[1]/td">
                        <xsl:for-each select="$colWidths">
                            <colspec colname="col{position()}"/>
                        </xsl:for-each>
                    </xsl:when>
                </xsl:choose>
                
                <xsl:for-each select="tr[@data-rowIsHeader]">
                    <thead>
                        <row>
                            <xsl:apply-templates/>
                        </row>
                    </thead>
                </xsl:for-each>
                <tbody>
                    <xsl:apply-templates/>
                </tbody>
            </tgroup>
            <!--<xsl:if test="../div/@id">
                <xsl:attribute name="id">
                    <xsl:value-of select="../div/@id"/>
                </xsl:attribute>
            </xsl:if>-->
        </table>
    </xsl:template>
    
    <!--<xsl:template name="mergeHeaderRows">
        <xsl:variable name="headerRows" select="." />
        <xsl:choose>
            <xsl:when test="count($headerRows) > 0">
                <tr>
                    <xsl:apply-templates select="$headerRows/th | $headerRows/td" />
                </tr>
            </xsl:when>
        </xsl:choose>
    </xsl:template>-->
    
    <xsl:template match="tr">
        <xsl:if test="not(@data-rowIsHeader)">
        <row>
            <xsl:apply-templates/>
        </row>
        </xsl:if>
    </xsl:template>
    
    <xsl:template match="th">
        <entry>
            <xsl:choose>
                <xsl:when test="p[contains(@class, 'left')]">
                    <xsl:attribute name="align" select="'left'"/>
                </xsl:when>
                <xsl:when test="span/p[contains(@class, 'left')]">
                    <xsl:attribute name="align" select="'left'"/>
                </xsl:when>
                <xsl:when test="p[contains(@class, 'right')]">
                    <xsl:attribute name="align" select="'right'"/>
                </xsl:when>
                <xsl:when test="span/p[contains(@class, 'right')]">
                    <xsl:attribute name="align" select="'right'"/>
                </xsl:when>
                <xsl:when test="p[contains(@class, 'center')]">
                    <xsl:attribute name="align" select="'center'"/>
                </xsl:when>
                <xsl:when test="span/p[contains(@class, 'center')]">
                    <xsl:attribute name="align" select="'center'"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:if test="p/@class[not(contains(., 'bold'))]">
                        <xsl:attribute name="align" select="p/@class[not(contains(., 'bold'))]"/>
                    </xsl:if>
                    <xsl:if test="span/p/@class[not(contains(., 'bold'))]">
                        <xsl:attribute name="align" select="span/p/@class[not(contains(., 'bold'))]"/>
                    </xsl:if>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:if test="@colspan">
                <xsl:variable name="trElem" select="../tr"/>
                <xsl:variable name="spanValue" select="@colspan"/>
                <xsl:variable name="precedingTcNumber" select="
                    sum(for $trElem in preceding-sibling::th
                    return
                    (if ($trElem/@colspan) then
                    ($trElem/@colspan)
                    else
                    (1)))"/>
                <xsl:attribute name="namest" select="concat('col', $precedingTcNumber + 1)"/>
                <xsl:attribute name="nameend" select="concat('col', $precedingTcNumber + $spanValue)"
                />
            </xsl:if>
            <xsl:apply-templates/>
        </entry>
    </xsl:template>
    
    <xsl:template match="td">
        <entry>
            <xsl:choose>
                <xsl:when test="p[contains(@class, 'left')]">
                    <xsl:attribute name="align" select="'left'"/>
                </xsl:when>
                <xsl:when test="span/p[contains(@class, 'left')]">
                    <xsl:attribute name="align" select="'left'"/>
                </xsl:when>
                <xsl:when test="p[contains(@class, 'right')]">
                    <xsl:attribute name="align" select="'right'"/>
                </xsl:when>
                <xsl:when test="span/p[contains(@class, 'right')]">
                    <xsl:attribute name="align" select="'right'"/>
                </xsl:when>
                <xsl:when test="p[contains(@class, 'center')]">
                    <xsl:attribute name="align" select="'center'"/>
                </xsl:when>
                <xsl:when test="span/p[contains(@class, 'center')]">
                    <xsl:attribute name="align" select="'center'"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:if test="p/@class[not(contains(., 'bold'))]">
                        <xsl:attribute name="align" select="p/@class[not(contains(., 'bold'))]"/>
                    </xsl:if>
                    <xsl:if test="span/p/@class[not(contains(., 'bold'))]">
                        <xsl:attribute name="align" select="span/p/@class[not(contains(., 'bold'))]"/>
                    </xsl:if>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:if test="@colspan">
                <xsl:variable name="trElem" select="../tr"/>
                <xsl:variable name="colspanValue" select="@colspan"/>
                <xsl:variable name="precedingTcNumber" select="
                    sum(for $trElem in preceding-sibling::td
                    return
                    (if ($trElem/@colspan) then
                    ($trElem/@colspan)
                    else
                    (1)))"/>
                <xsl:attribute name="namest" select="concat('col', $precedingTcNumber + 1)"/>
                <xsl:attribute name="nameend"
                    select="concat('col', $precedingTcNumber + $colspanValue)"/>
            </xsl:if>
            
            <xsl:choose>
                <xsl:when test="p/@class[contains(., 'bold')]">
                    <b><xsl:apply-templates/></b>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates/>
                </xsl:otherwise>
            </xsl:choose>
            
        </entry>
    </xsl:template>
    
    <xsl:template match="span">
        <xsl:choose>
            <xsl:when test="@style ='font-style:italic;'">
                <i>
                    <xsl:apply-templates/>
                </i>
            </xsl:when>
            <xsl:when test="@style ='font-weight:bold;'">
                <b>
                    <xsl:apply-templates/>
                </b>
            </xsl:when>
            <xsl:when test="@class ='Superscript'">
                <sup>
                    <xsl:apply-templates/>
                </sup>
            </xsl:when>
            <xsl:otherwise>
                <!--<xsl:message select="'Span style not handle'"/>-->
                <xsl:apply-templates/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
        
    <xsl:template match="object">
        
        <xsl:variable name="width" select="substring-before(normalize-space(@style), ';')"/>
        <xsl:variable name="height" select="substring-after(normalize-space(@style), ';')"/>
        <data>
            <object>
                <xsl:if test="@name">
                    <xsl:attribute name="name">
                        <xsl:value-of select="@name"/>
                    </xsl:attribute>
                </xsl:if>
                <xsl:choose>
                    <xsl:when test="contains(@style, 'width:') and contains(@style, 'height:')">
                        <xsl:attribute name="width" select="replace($width, 'width:', '')"/>
                        <xsl:attribute name="height" select="replace(replace($height, ';', ''), 'height:', '')"/>                        
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:message select="'Please check @style value in object element'"/>
                    </xsl:otherwise>
                </xsl:choose>
                <xsl:apply-templates/>
            </object>
        </data>
    </xsl:template>
    
    <xsl:template match="a[not(@href) and not(@name) and not(normalize-space())]"/>
    
    <xsl:template match="a">
        <xsl:variable name="href" select="substring-after(@href, '#')"/>
        <xsl:if test="normalize-space()">
        <pwc-xref>
            <xsl:choose>
                <xsl:when test="starts-with(@href, '../') and contains(@href, '.')">
                    <xsl:attribute name="href" select="concat('#', $href)"/>
                </xsl:when>
                <xsl:when test="starts-with($href, 'db')">
                    <xsl:attribute name="href" select="concat('#', $href)"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:attribute name="href" select="@href"/>
                    <xsl:attribute name="scope" select="'external'"/>
                    <xsl:attribute name="format" select="'html'"/>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:apply-templates/>
        </pwc-xref>
        </xsl:if>
    </xsl:template>
    
    <xsl:template match="br"/>
</xsl:stylesheet>
