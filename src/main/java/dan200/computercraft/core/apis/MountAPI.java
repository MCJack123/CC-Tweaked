/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis;

import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.filesystem.FileMount;
import dan200.computercraft.core.filesystem.FileSystem;
import dan200.computercraft.core.filesystem.FileSystemException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MountAPI implements ILuaAPI 
{

    private FileSystem fs;
    private final Map<String, IWritableMount> mounts = new HashMap<>();
    private final Map<String, Boolean> isReadOnly = new HashMap<>();

    public MountAPI( FileSystem fs ) 
    {
        this.fs = fs;
    }

    void setFileSystem( FileSystem fs ) 
    {
        this.fs = fs;
    }

    @Override
    public String[] getNames() 
    {
        return new String[]{ "mounter" };
    }

    @Override
    public void startup() 
    {

    }

    @Override
    public void update() 
    {

    }

    @Override
    public void shutdown() 
    {
        for ( String key : mounts.keySet() ) fs.unmount( key );
        mounts.clear();
        isReadOnly.clear();
    }

    @Nonnull
    @Override
    public String[] getMethodNames() 
    {
        return new String[]{ "mount", "unmount", "list", "isReadOnly" };
    }

    @Nullable
    @Override
    public Object[] callMethod( @Nonnull ILuaContext context, int method, @Nonnull Object[] arguments ) throws LuaException 
    {
        if( fs == null ) 
        {
            throw new LuaException( "File system not initialized" );
        }
        try 
        {
            switch ( method ) 
            {
                case 0:
                    // mount(name, path, writable)
                    if( arguments.length < 2 ) 
                    {
                        throw new LuaException( "Expected at least 2 arguments, got " + arguments.length );
                    }
                    if( !(arguments[0] instanceof String) ) 
                    {
                        throw new LuaException( "Expected argument #1 to be string, got " + arguments[0].getClass().getName() );
                    }
                    if( !(arguments[1] instanceof String) ) 
                    {
                        throw new LuaException( "Expected argument #2 to be string, got " + arguments[0].getClass().getName() );
                    }
                    File f;
                    IWritableMount mount;
                    try 
                    {
                        f = new File( (String)arguments[1] );
                        mount = new FileMount( f, 1000000000, true );
                    } 
                    catch( NullPointerException e ) 
                    {
                        e.printStackTrace();
                        throw new LuaException( "Error while creating mount" );
                    }
                    if( arguments.length > 2 && !(boolean)arguments[2] ) 
                    {
                        try 
                        {
                            fs.mount( (String)arguments[0], "/" + arguments[0], mount );
                            isReadOnly.put( (String)arguments[0], true );
                        } 
                        catch( FileSystemException e ) 
                        {
                            throw new LuaException( "Could not mount drive: " + e.getMessage() );
                        }
                    } 
                    else 
                    {
                        try 
                        {
                            fs.mountWritable( (String)arguments[0], "/" + arguments[0], mount );
                            isReadOnly.put( (String)arguments[0], false );
                        } 
                        catch( FileSystemException e ) 
                        {
                            throw new LuaException( "Could not mount drive read-write: " + e.getMessage() );
                        }
                    }
                    mounts.put( (String)arguments[0], mount );
                    break;
                case 1:
                    // unmount(name)
                    if( arguments.length != 1 ) 
                    {
                        throw new LuaException( "Expected 1 argument, got " + arguments.length );
                    }
                    if( !(arguments[0] instanceof String) ) 
                    {
                        throw new LuaException( "Expected argument #1 to be string" );
                    }
                    IWritableMount m = mounts.get( arguments[0] );
                    if( m == null ) 
                    {
                        throw new LuaException( "Could not find mount " + arguments[0] );
                    }
                    fs.unmount( "/" + arguments[0] );
                    mounts.remove( arguments[0] );
                    isReadOnly.remove( arguments[0] );
                    break;
                case 2:
                    // list()
                    Map<Object, Object> retval = new HashMap<>();
                    for ( String key : mounts.keySet() ) retval.put( key, ((FileMount)mounts.get( key )).getRootPath().getAbsolutePath() );
                    return new Object[]{ retval };
                case 3:
                    return new Object[]{ isReadOnly.get( arguments[0] ) };
            }
        } 
        catch( NullPointerException e ) 
        {
            e.printStackTrace();
        }
        return null;
    }
}
