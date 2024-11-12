<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xd="http://www.oxygenxml.com/ns/doc/xsl"
    exclude-result-prefixes="xs xd" version="2.0">

    <!--<xsl:output indent="yes" method="xml" doctype-public="-//OASIS//DTD DITA Topic//EN"
        doctype-system="topic.dtd"/>-->

    <xsl:template match="@* | node()">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="file[@heading and @level]">

        <xsl:variable name="heading1" select="tokenize(@heading, '\s+')"/>
        <xsl:variable name="first_10_words1"
            select="string-join($heading1[position() &lt;= 10], ' ')"/>
        <xsl:variable name="Title1"
            select="translate($first_10_words1, translate($first_10_words1, 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 ', ''), '')"/>
        <xsl:variable name="title_heading1" select="replace($Title1, ' ', '_')"/>

        <xsl:variable name="uri" select="substring-after(@uri_fragment, '#')"/>

        <xsl:result-document href="cpa/{$title_heading1}/{$title_heading1}_{$uri}.dita">

            <file>

                <xsl:apply-templates select="@* | node() except (children)"/>
            </file>

            <xsl:for-each select="children/file[@heading and @level]">
                <xsl:variable name="heading2" select="tokenize(@heading, '\s+')"/>
                <xsl:variable name="first_10_words2"
                    select="string-join($heading2[position() &lt;= 10], ' ')"/>
                <xsl:variable name="Title2"
                    select="translate($first_10_words2, translate($first_10_words2, 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 ', ''), '')"/>
                <xsl:variable name="title_heading2" select="replace($Title2, ' ', '_')"/>

                <xsl:variable name="uri" select="substring-after(@uri_fragment, '#')"/>

                <xsl:result-document
                    href="cpa/{$title_heading1}/{$title_heading2}/{$title_heading2}_{$uri}.dita">
                    <file>

                        <xsl:apply-templates select="@* | node() except (children)"/>
                    </file>

                    <xsl:for-each select="children/file[@heading and @level]">
                        <xsl:variable name="heading3" select="tokenize(@heading, '\s+')"/>
                        <xsl:variable name="first_10_words3"
                            select="string-join($heading3[position() &lt;= 10], ' ')"/>
                        <xsl:variable name="Title3"
                            select="translate($first_10_words3, translate($first_10_words3, 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 ', ''), '')"/>
                        <xsl:variable name="title_heading3" select="replace($Title3, ' ', '_')"/>

                        <xsl:variable name="uri" select="substring-after(@uri_fragment, '#')"/>

                        <xsl:result-document
                            href="cpa/{$title_heading1}/{$title_heading2}/{$title_heading3}/{$title_heading3}_{$uri}.dita">
                            <file>

                                <xsl:apply-templates select="@* | node() except (children)"/>
                            </file>

                            <xsl:for-each select="children/file[@heading and @level]">
                                <xsl:variable name="heading4" select="tokenize(@heading, '\s+')"/>
                                <xsl:variable name="first_10_words4"
                                    select="string-join($heading4[position() &lt;= 10], ' ')"/>
                                <xsl:variable name="Title4"
                                    select="translate($first_10_words4, translate($first_10_words4, 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 ', ''), '')"/>
                                <xsl:variable name="title_heading4"
                                    select="replace($Title4, ' ', '_')"/>

                                <xsl:variable name="uri"
                                    select="substring-after(@uri_fragment, '#')"/>

                                <xsl:result-document
                                    href="cpa/{$title_heading1}/{$title_heading2}/{$title_heading3}/{$title_heading4}/{$title_heading4}_{$uri}.dita">
                                    <file>

                                        <xsl:apply-templates select="@* | node() except (children)"
                                        />
                                    </file>

                                    <xsl:for-each select="children/file[@heading and @level]">
                                        <xsl:variable name="heading5"
                                            select="tokenize(@heading, '\s+')"/>
                                        <xsl:variable name="first_10_words5"
                                            select="string-join($heading5[position() &lt;= 10], ' ')"/>
                                        <xsl:variable name="Title5"
                                            select="translate($first_10_words5, translate($first_10_words5, 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 ', ''), '')"/>
                                        <xsl:variable name="title_heading5"
                                            select="replace($Title5, ' ', '_')"/>

                                        <xsl:variable name="uri"
                                            select="substring-after(@uri_fragment, '#')"/>

                                        <xsl:result-document
                                            href="cpa/{$title_heading1}/{$title_heading2}/{$title_heading3}/{$title_heading4}/{$title_heading5}/{$title_heading5}_{$uri}.dita">
                                            <file>

                                                <xsl:apply-templates
                                                  select="@* | node() except (children)"/>
                                            </file>

                                            <xsl:for-each
                                                select="children/file[@heading and @level]">
                                                <xsl:variable name="heading6"
                                                  select="tokenize(@heading, '\s+')"/>
                                                <xsl:variable name="first_10_words6"
                                                  select="string-join($heading6[position() &lt;= 10], ' ')"/>
                                                <xsl:variable name="Title6"
                                                  select="translate($first_10_words6, translate($first_10_words6, 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 ', ''), '')"/>
                                                <xsl:variable name="title_heading6"
                                                  select="replace($Title6, ' ', '_')"/>

                                                <xsl:variable name="uri"
                                                  select="substring-after(@uri_fragment, '#')"/>

                                                <xsl:result-document
                                                  href="cpa/{$title_heading1}/{$title_heading2}/{$title_heading3}/{$title_heading4}/{$title_heading5}/topics/{$title_heading6}_{$uri}.dita">
                                                  <file>

                                                  <xsl:apply-templates
                                                  select="@* | node() except (children)"/>
                                                  </file>

                                                  <xsl:for-each
                                                  select="children/file[@heading and @level]">
                                                  <xsl:variable name="heading7"
                                                  select="tokenize(@heading, '\s+')"/>
                                                  <xsl:variable name="first_10_words7"
                                                  select="string-join($heading7[position() &lt;= 10], ' ')"/>
                                                  <xsl:variable name="Title7"
                                                  select="translate($first_10_words7, translate($first_10_words7, 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 ', ''), '')"/>
                                                  <xsl:variable name="title_heading7"
                                                  select="replace($Title7, ' ', '_')"/>

                                                  <xsl:variable name="uri"
                                                  select="substring-after(@uri_fragment, '#')"/>

                                                  <xsl:result-document
                                                  href="cpa/{$title_heading1}/{$title_heading2}/{$title_heading3}/{$title_heading4}/{$title_heading5}/topics/{$title_heading7}_{$uri}.dita">
                                                  <file>

                                                  <xsl:apply-templates
                                                  select="@* | node() except (children)"/>
                                                  </file>

                                                  <xsl:for-each
                                                  select="children/file[@heading and @level]">
                                                  <xsl:variable name="heading8"
                                                  select="tokenize(@heading, '\s+')"/>
                                                  <xsl:variable name="first_10_words8"
                                                  select="string-join($heading8[position() &lt;= 10], ' ')"/>
                                                  <xsl:variable name="Title8"
                                                  select="translate($first_10_words8, translate($first_10_words8, 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 ', ''), '')"/>
                                                  <xsl:variable name="title_heading8"
                                                  select="replace($Title8, ' ', '_')"/>

                                                  <xsl:variable name="uri"
                                                  select="substring-after(@uri_fragment, '#')"/>

                                                  <xsl:result-document
                                                  href="cpa/{$title_heading1}/{$title_heading2}/{$title_heading3}/{$title_heading4}/{$title_heading5}/topics/{$title_heading8}_{$uri}.dita">
                                                  <file>

                                                  <xsl:apply-templates
                                                  select="@* | node() except (children)"/>
                                                  </file>

                                                  <xsl:for-each
                                                  select="children/file[@heading and @level]">
                                                  <xsl:variable name="heading9"
                                                  select="tokenize(@heading, '\s+')"/>
                                                  <xsl:variable name="first_10_words9"
                                                  select="string-join($heading9[position() &lt;= 10], ' ')"/>
                                                  <xsl:variable name="Title9"
                                                  select="translate($first_10_words9, translate($first_10_words9, 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 ', ''), '')"/>
                                                  <xsl:variable name="title_heading9"
                                                  select="replace($Title9, ' ', '_')"/>

                                                  <xsl:variable name="uri"
                                                  select="substring-after(@uri_fragment, '#')"/>

                                                  <xsl:result-document
                                                  href="cpa/{$title_heading1}/{$title_heading2}/{$title_heading3}/{$title_heading4}/{$title_heading5}/topics/{$title_heading9}_{$uri}.dita">
                                                  <file>

                                                  <xsl:apply-templates
                                                  select="@* | node() except (children)"/>
                                                  </file>

                                                  <xsl:for-each
                                                  select="children/file[@heading and @level]">
                                                  <xsl:variable name="heading10"
                                                  select="tokenize(@heading, '\s+')"/>
                                                  <xsl:variable name="first_10_words10"
                                                  select="string-join($heading10[position() &lt;= 10], ' ')"/>
                                                  <xsl:variable name="Title10"
                                                  select="translate($first_10_words10, translate($first_10_words10, 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 ', ''), '')"/>
                                                  <xsl:variable name="title_heading10"
                                                  select="replace($Title10, ' ', '_')"/>

                                                  <xsl:variable name="uri"
                                                  select="substring-after(@uri_fragment, '#')"/>

                                                  <xsl:result-document
                                                  href="cpa/{$title_heading1}/{$title_heading2}/{$title_heading3}/{$title_heading4}/{$title_heading5}/topics/{$title_heading10}_{$uri}.dita">
                                                  <file>

                                                  <xsl:apply-templates
                                                  select="@* | node() except (children)"/>
                                                  </file>

                                                  <xsl:for-each
                                                  select="children/file[@heading and @level]">
                                                  <xsl:variable name="heading11"
                                                  select="tokenize(@heading, '\s+')"/>
                                                  <xsl:variable name="first_10_words11"
                                                  select="string-join($heading11[position() &lt;= 10], ' ')"/>
                                                  <xsl:variable name="Title11"
                                                  select="translate($first_10_words11, translate($first_10_words11, 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 ', ''), '')"/>
                                                  <xsl:variable name="title_heading11"
                                                  select="replace($Title11, ' ', '_')"/>

                                                  <xsl:variable name="uri"
                                                  select="substring-after(@uri_fragment, '#')"/>

                                                  <xsl:result-document
                                                  href="cpa/{$title_heading1}/{$title_heading2}/{$title_heading3}/{$title_heading4}/{$title_heading5}/topics/{$title_heading11}_{$uri}.dita">
                                                  <file>

                                                  <xsl:apply-templates
                                                  select="@* | node() except (children)"/>
                                                  </file>

                                                  <xsl:for-each
                                                  select="children/file[@heading and @level]">
                                                  <xsl:variable name="heading12"
                                                  select="tokenize(@heading, '\s+')"/>
                                                  <xsl:variable name="first_10_words12"
                                                  select="string-join($heading12[position() &lt;= 10], ' ')"/>
                                                  <xsl:variable name="Title12"
                                                  select="translate($first_10_words12, translate($first_10_words12, 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 ', ''), '')"/>
                                                  <xsl:variable name="title_heading12"
                                                  select="replace($Title12, ' ', '_')"/>

                                                  <xsl:variable name="uri"
                                                  select="substring-after(@uri_fragment, '#')"/>

                                                  <xsl:result-document
                                                  href="cpa/{$title_heading1}/{$title_heading2}/{$title_heading3}/{$title_heading4}/{$title_heading5}/topics/{$title_heading12}_{$uri}.dita">
                                                  <file>

                                                  <xsl:apply-templates
                                                  select="@* | node() except (children)"/>
                                                  </file>

                                                  <xsl:for-each
                                                  select="children/file[@heading and @level]">
                                                  <xsl:variable name="heading13"
                                                  select="tokenize(@heading, '\s+')"/>
                                                  <xsl:variable name="first_10_words13"
                                                  select="string-join($heading13[position() &lt;= 10], ' ')"/>
                                                  <xsl:variable name="Title13"
                                                  select="translate($first_10_words13, translate($first_10_words13, 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 ', ''), '')"/>
                                                  <xsl:variable name="title_heading13"
                                                  select="replace($Title13, ' ', '_')"/>

                                                  <xsl:variable name="uri"
                                                  select="substring-after(@uri_fragment, '#')"/>

                                                  <xsl:result-document
                                                  href="cpa/{$title_heading1}/{$title_heading2}/{$title_heading3}/{$title_heading4}/{$title_heading5}/topics/{$title_heading13}_{$uri}.dita">
                                                  <file>

                                                  <xsl:apply-templates
                                                  select="@* | node() except (children)"/>
                                                  </file>

                                                  </xsl:result-document>
                                                  </xsl:for-each>
                                                  </xsl:result-document>
                                                  </xsl:for-each>
                                                  </xsl:result-document>
                                                  </xsl:for-each>
                                                  </xsl:result-document>
                                                  </xsl:for-each>
                                                  </xsl:result-document>
                                                  </xsl:for-each>
                                                  </xsl:result-document>
                                                  </xsl:for-each>
                                                  </xsl:result-document>
                                                  </xsl:for-each>
                                                </xsl:result-document>
                                            </xsl:for-each>
                                        </xsl:result-document>
                                    </xsl:for-each>
                                </xsl:result-document>
                            </xsl:for-each>
                        </xsl:result-document>
                    </xsl:for-each>
                </xsl:result-document>
            </xsl:for-each>
        </xsl:result-document>
    </xsl:template>

</xsl:stylesheet>
