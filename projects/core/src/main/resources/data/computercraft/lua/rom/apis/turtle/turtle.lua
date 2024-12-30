-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

--- @module turtle

if  not turtle then
    error("Cannot load turtle API on computer", 2)
end

--- The builtin turtle API, without any generated helper functions.
--
-- @deprecated Historically this table behaved differently to the main turtle API, but this is no longer the case. You
-- should not need to use it.
local native = turtle.native or turtle

-- XXXX what if workbench was taken by player?
local function addCraftMethod(object)
    if  (peripheral.getType("left") == "workbench") then
        object.craft = function(...)
            return peripheral.call("left", "craft", ...)
        end
    elseif(peripheral.getType("right") == "workbench") then
        object.craft = function(...)
            return peripheral.call("right", "craft", ...)
        end
    else
        object.craft = nil
    end
end

local turtle = {native = native}

for k, v in pairs(native) do
    if  ((k == "equipLeft") or (k == "equipRight")) then
        turtle[k] = function(...)
            local result, err = v(...)
            addCraftMethod(turtle)
            return result, err
        end
    else
        turtle[k] = v
    end
end
addCraftMethod(turtle)

return turtle
