/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.terminal;

import dan200.computercraft.shared.util.Palette;
import net.minecraft.nbt.NBTTagCompound;

public class Terminal
{
    private static final String base16 = "0123456789abcdef";

    private int m_cursorX;
    private int m_cursorY;
    private boolean m_cursorBlink;
    private int m_cursorColour;
    private int m_cursorBackgroundColour;

    private int m_width;
    private int m_height;

    private TextBuffer[] m_text;
    private TextBuffer[] m_textColour;
    private TextBuffer[] m_backgroundColour;
    private TextBuffer[] m_pixelColor;

    private final Palette m_palette;

    private boolean m_changed;
    private boolean m_pixelMode;
    private final Runnable onChanged;

    public Terminal( int width, int height )
    {
        this( width, height, null );
    }

    public Terminal( int width, int height, Runnable changedCallback )
    {
        m_width = width;
        m_height = height;
        onChanged = changedCallback;

        m_cursorColour = 0;
        m_cursorBackgroundColour = 15;

        m_text = new TextBuffer[m_height];
        m_textColour = new TextBuffer[m_height];
        m_backgroundColour = new TextBuffer[m_height];
        m_pixelColor = new TextBuffer[m_height * 9];
        for( int i = 0; i < m_height; i++ )
        {
            m_text[i] = new TextBuffer( ' ', m_width );
            m_textColour[i] = new TextBuffer( base16.charAt( m_cursorColour ), m_width );
            m_backgroundColour[i] = new TextBuffer( base16.charAt( m_cursorBackgroundColour ), m_width );
            for ( int j = 0; j < 9; j++ ) 
            {
                m_pixelColor[i * 9 + j] = new TextBuffer( (char)15, m_width * 6 );
            }
        }

        m_cursorX = 0;
        m_cursorY = 0;
        m_cursorBlink = false;

        m_changed = false;
        m_pixelMode = false;

        m_palette = new Palette();
    }

    public synchronized void reset()
    {
        m_cursorColour = 0;
        m_cursorBackgroundColour = 15;
        m_cursorX = 0;
        m_cursorY = 0;
        m_cursorBlink = false;
        m_pixelMode = false;
        clear();
        setChanged();
        m_palette.resetColours();
    }

    public int getWidth()
    {
        return m_width;
    }

    public int getHeight()
    {
        return m_height;
    }

    public synchronized void resize( int width, int height )
    {
        if( width == m_width && height == m_height )
        {
            return;
        }

        int oldHeight = m_height;
        int oldWidth = m_width;
        TextBuffer[] oldText = m_text;
        TextBuffer[] oldTextColour = m_textColour;
        TextBuffer[] oldBackgroundColour = m_backgroundColour;
        TextBuffer[] oldPixelColor = m_pixelColor;

        m_width = width;
        m_height = height;

        m_text = new TextBuffer[m_height];
        m_textColour = new TextBuffer[m_height];
        m_backgroundColour = new TextBuffer[m_height];
        m_pixelColor = new TextBuffer[ m_height * 9 ];
        for( int i = 0; i < m_height; i++ )
        {
            if( i >= oldHeight )
            {
                m_text[i] = new TextBuffer( ' ', m_width );
                m_textColour[i] = new TextBuffer( base16.charAt( m_cursorColour ), m_width );
                m_backgroundColour[i] = new TextBuffer( base16.charAt( m_cursorBackgroundColour ), m_width );
                for ( int j = 0; j < 9; j++ ) 
                {
                    m_pixelColor[ i * 9 + j] = new TextBuffer( (char)15, m_width * 6 );
                }
            }
            else if( m_width == oldWidth )
            {
                m_text[i] = oldText[i];
                m_textColour[i] = oldTextColour[i];
                m_backgroundColour[i] = oldBackgroundColour[i];
                for ( int j = 0; j < 9; j++ )
                {
                    m_pixelColor[i * 9 + j] = oldPixelColor[i];
                }
            }
            else
            {
                m_text[i] = new TextBuffer( ' ', m_width );
                m_textColour[i] = new TextBuffer( base16.charAt( m_cursorColour ), m_width );
                m_backgroundColour[i] = new TextBuffer( base16.charAt( m_cursorBackgroundColour ), m_width );
                for ( int j = 0; j < 9; j++ )
                {
                    m_pixelColor[i * 9 + j] = new TextBuffer( (char)15, m_width * 6 );
                }
                m_text[i].write( oldText[i] );
                m_textColour[i].write( oldTextColour[i] );
                m_backgroundColour[i].write( oldBackgroundColour[i] );
                for ( int j = 0; j < 9; j++ )
                {
                    m_pixelColor[i * 9 + j].write( oldPixelColor[i] );
                }
            }
        }
        setChanged();
    }

    public void setCursorPos( int x, int y )
    {
        if( m_cursorX != x || m_cursorY != y )
        {
            m_cursorX = x;
            m_cursorY = y;
            setChanged();
        }
    }

    public void setCursorBlink( boolean blink )
    {
        if( m_cursorBlink != blink )
        {
            m_cursorBlink = blink;
            setChanged();
        }
    }

    public void setTextColour( int colour )
    {
        if( m_cursorColour != colour )
        {
            m_cursorColour = colour;
            setChanged();
        }
    }

    public void setBackgroundColour( int colour )
    {
        if( m_cursorBackgroundColour != colour )
        {
            m_cursorBackgroundColour = colour;
            setChanged();
        }
    }

    public int getCursorX()
    {
        return m_cursorX;
    }

    public int getCursorY()
    {
        return m_cursorY;
    }

    public boolean getCursorBlink()
    {
        return m_cursorBlink;
    }

    public int getTextColour()
    {
        return m_cursorColour;
    }

    public int getBackgroundColour()
    {
        return m_cursorBackgroundColour;
    }

    public Palette getPalette()
    {
        return m_palette;
    }

    public synchronized void blit( String text, String textColour, String backgroundColour )
    {
        int x = m_cursorX;
        int y = m_cursorY;
        if( y >= 0 && y < m_height )
        {
            m_text[y].write( text, x );
            m_textColour[y].write( textColour, x );
            m_backgroundColour[y].write( backgroundColour, x );
            setChanged();
        }
    }

    public synchronized void write( String text )
    {
        int x = m_cursorX;
        int y = m_cursorY;
        if( y >= 0 && y < m_height )
        {
            m_text[y].write( text, x );
            m_textColour[y].fill( base16.charAt( m_cursorColour ), x, x + text.length() );
            m_backgroundColour[y].fill( base16.charAt( m_cursorBackgroundColour ), x, x + text.length() );
            setChanged();
        }
    }

    public synchronized void scroll( int yDiff )
    {
        if( yDiff != 0 )
        {
            TextBuffer[] newText = new TextBuffer[m_height];
            TextBuffer[] newTextColour = new TextBuffer[m_height];
            TextBuffer[] newBackgroundColour = new TextBuffer[m_height];
            for( int y = 0; y < m_height; y++ )
            {
                int oldY = y + yDiff;
                if( oldY >= 0 && oldY < m_height )
                {
                    newText[y] = m_text[oldY];
                    newTextColour[y] = m_textColour[oldY];
                    newBackgroundColour[y] = m_backgroundColour[oldY];
                }
                else
                {
                    newText[y] = new TextBuffer( ' ', m_width );
                    newTextColour[y] = new TextBuffer( base16.charAt( m_cursorColour ), m_width );
                    newBackgroundColour[y] = new TextBuffer( base16.charAt( m_cursorBackgroundColour ), m_width );
                }
            }
            m_text = newText;
            m_textColour = newTextColour;
            m_backgroundColour = newBackgroundColour;
            setChanged();
        }
    }

    public synchronized void clear()
    {
        for( int y = 0; y < m_height; y++ )
        {
            m_text[y].fill( ' ' );
            m_textColour[y].fill( base16.charAt( m_cursorColour ) );
            m_backgroundColour[y].fill( base16.charAt( m_cursorBackgroundColour ) );
            for ( int j = 0; j < 9; j++ )
            {
                m_pixelColor[ y * 9 + j].fill( (char)15 );
            }
        }
        setChanged();
    }

    public synchronized void clearLine()
    {
        int y = m_cursorY;
        if( y >= 0 && y < m_height )
        {
            m_text[y].fill( ' ' );
            m_textColour[y].fill( base16.charAt( m_cursorColour ) );
            m_backgroundColour[y].fill( base16.charAt( m_cursorBackgroundColour ) );
            setChanged();
        }
    }

    public synchronized TextBuffer getLine( int y )
    {
        if( y >= 0 && y < m_height )
        {
            return m_text[y];
        }
        return null;
    }

    public synchronized void setLine( int y, String text, String textColour, String backgroundColour )
    {
        m_text[y].write( text );
        m_textColour[y].write( textColour );
        m_backgroundColour[y].write( backgroundColour );
        setChanged();
    }

    public synchronized TextBuffer getTextColourLine( int y )
    {
        if( y >= 0 && y < m_height )
        {
            return m_textColour[y];
        }
        return null;
    }

    public synchronized TextBuffer getBackgroundColourLine( int y )
    {
        if( y >= 0 && y < m_height )
        {
            return m_backgroundColour[y];
        }
        return null;
    }

    /**
     * @deprecated All {@code *Changed()} methods are deprecated: one should pass in a callback
     * instead.
     */
    @Deprecated
    public final boolean getChanged()
    {
        return m_changed;
    }

    public final void setChanged()
    {
        m_changed = true;
        if( onChanged != null ) onChanged.run();
    }

    public final void clearChanged()
    {
        m_changed = false;
    }

    public synchronized NBTTagCompound writeToNBT( NBTTagCompound nbt )
    {
        nbt.setInteger( "term_cursorX", m_cursorX );
        nbt.setInteger( "term_cursorY", m_cursorY );
        nbt.setBoolean( "term_cursorBlink", m_cursorBlink );
        nbt.setBoolean( "term_graphicsMode", m_pixelMode );
        nbt.setInteger( "term_textColour", m_cursorColour );
        nbt.setInteger( "term_bgColour", m_cursorBackgroundColour );
        for( int n = 0; n < m_height; n++ )
        {
            nbt.setString( "term_text_" + n, m_text[n].toString() );
            nbt.setString( "term_textColour_" + n, m_textColour[n].toString() );
            nbt.setString( "term_textBgColour_" + n, m_backgroundColour[n].toString() );
            for ( int m = 0; m < 9; m++ ) 
            {
                nbt.setString( "term_pixelColour_" + (n * 9 + m), m_pixelColor[ n * 9 + m ].toString() );
            }
        }
        if( m_palette != null )
        {
            m_palette.writeToNBT( nbt );
        }
        return nbt;
    }

    public synchronized void readFromNBT( NBTTagCompound nbt )
    {
        m_cursorX = nbt.getInteger( "term_cursorX" );
        m_cursorY = nbt.getInteger( "term_cursorY" );
        m_cursorBlink = nbt.getBoolean( "term_cursorBlink" );
        m_cursorColour = nbt.getInteger( "term_textColour" );
        m_cursorBackgroundColour = nbt.getInteger( "term_bgColour" );
        m_pixelMode = nbt.getBoolean( "term_graphicsMode" );

        for( int n = 0; n < m_height; n++ )
        {
            m_text[n].fill( ' ' );
            if( nbt.hasKey( "term_text_" + n ) )
            {
                m_text[n].write( nbt.getString( "term_text_" + n ) );
            }
            m_textColour[n].fill( base16.charAt( m_cursorColour ) );
            if( nbt.hasKey( "term_textColour_" + n ) )
            {
                m_textColour[n].write( nbt.getString( "term_textColour_" + n ) );
            }
            m_backgroundColour[n].fill( base16.charAt( m_cursorBackgroundColour ) );
            if( nbt.hasKey( "term_textBgColour_" + n ) )
            {
                m_backgroundColour[n].write( nbt.getString( "term_textBgColour_" + n ) );
            }
            for ( int m = 0; m < 9; m++ ) 
            {
                m_pixelColor[n * 9 + m].fill( (char)0 );
                if( nbt.hasKey( "term_pixelColour_" + (n * 9 + m) ) )
                {
                    m_pixelColor[n * 9 + m].write( nbt.getString( "term_pixelColour_" + (n * 9 + m) ) );
                }
            }
        }
        if( m_palette != null )
        {
            m_palette.readFromNBT( nbt );
        }
        setChanged();
    }

    public void setGraphicsMode( boolean graphicsMode ) 
    {
        this.m_pixelMode = graphicsMode;
        m_changed = true;
    }

    public boolean getGraphicsMode() 
    {
        return m_pixelMode;
    }

    public void setPixel( int x, int y, char colour ) 
    {
        m_pixelColor[y].setChar( x, colour );
        m_changed = true;
    }

    public char getPixel( int x, int y ) 
    {
        return m_pixelColor[y].charAt( x );
    }

    public TextBuffer getPixelLine( int y ) 
    {
        if( y >= 0 && y < m_height * 9 )
        {
            return m_pixelColor[y];
        }
        return null;
    }
}
