project = 'CC: Tweaked'
description = 'Just another ComputerCraft fork'

full_description = [[
CC: Tweaked is a fork of ComputerCraft, adding programmable computers, turtles
and more to Minecraft.
]]

dir = 'build-doc'
format = 'markdown'
file = {
    'src/main/resources/assets/computercraft/lua/bios.lua',
    'src/main/resources/assets/computercraft/lua/rom',
    exclude = {
        'src/main/resources/assets/computercraft/lua/rom/programs',
    },
}

no_space_before_args = true
backtick_references = false

template = 'doc'
style = 'doc'
