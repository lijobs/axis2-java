<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="text"/>
    <xsl:template match="/interface">
    package <xsl:value-of select="@package"/>;

    /*
     *  Auto generated java interface by the Axis code generator
    */

    public interface <xsl:value-of select="@name"></xsl:value-of> {
     <xsl:for-each select="method">
        public <xsl:value-of select="output/param/@type"></xsl:value-of><xsl:text> </xsl:text><xsl:value-of select="@name"></xsl:value-of>(<xsl:value-of select="input/param/@type"></xsl:value-of><xsl:text> </xsl:text><xsl:value-of select="output/param/@name"></xsl:value-of>) throws java.rmi.RemoteException;
     </xsl:for-each>
    }
    </xsl:template>
 </xsl:stylesheet>