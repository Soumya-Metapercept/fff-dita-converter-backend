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

    <!-- Template to handle the table and merge the thead elements -->
    <xsl:template match="table">
        <xsl:copy>
            <xsl:copy-of select="@*"/>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>

    <!-- Template to handle the tgroup and merge the thead elements -->
    <xsl:template match="tgroup">
        <xsl:copy>
            <xsl:apply-templates select="@*"/>
            <xsl:apply-templates select="colspec"/>
            <!-- Merge the thead elements -->
            <xsl:if test="thead">
                <thead>
                    <xsl:apply-templates select="thead/row"/>
                </thead>
            </xsl:if>
            <!-- Apply templates for the rest of the tgroup content -->
            <xsl:apply-templates select="node() except (thead | colspec)"/>
        </xsl:copy>
    </xsl:template>

    <!-- Remove original thead elements to avoid duplication -->
    <xsl:template match="thead"/>
</xsl:stylesheet>
