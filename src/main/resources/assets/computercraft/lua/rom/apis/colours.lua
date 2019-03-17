--- Colours for lovers of British spelling.
--
-- @see colors
-- @module colours

local colours = _ENV
for k,v in pairs(colors) do
	colours[k] = v
end

--- Grey. 0x80 in hexadecimal, displayed as `7` in paint files and `term.blit`,
-- has a default terminal colour of `#4C4C4C`.
--
-- @see colors.gray
colours.grey = colors.gray
colours.gray = nil

--- Light grey. 0x100 in hexadecimal, displayed as `8` in paint files and
-- `term.blit`, has a default terminal colour of `#999999`.
--
-- @see colors.lightGray
colours.lightGrey = colors.lightGray
colours.lightGray = nil
