package com.mwooten.chime.worlds

/**
 * What the companion looks like in Simple mode. The look is the body; the [Finish] is the
 * material it's cast in. Both are independent of the personality, which keeps the voice and face.
 */
enum class Look(val title: String, val defaultFinish: Finish) {
    STAR("Pocket Star", Finish.AMBER),
    SPRITE("Petal Sprite", Finish.SEA_GLASS),
    MOTH("Lantern Moth", Finish.MOONSTONE),
    EMBER("Ember Wisp", Finish.OPAL),
    BUBBLE("Bubble Buddy", Finish.ABALONE),
    PUFF("Puff Cloud", Finish.ROSE_QUARTZ);

    companion object {
        fun byName(name: String?) = entries.firstOrNull { it.name == name } ?: STAR
    }
}

/**
 * Ten natural materials that catch the light. [c] is the body, [light] the highlight, [dark] the
 * shadow, and [sheen] the color of the moving gleam ([sheenKind] "solid"), or null when the gleam
 * is a hue-cycling "rainbow" or "bismuth" fire.
 */
enum class Finish(val title: String, val about: String, val c: Long, val light: Long, val dark: Long, val sheen: Long?, val sheenKind: String) {
    AMBER("Amber", "Tree resin held up to a window. Warm honey with a bright gold edge.", 0xFFE09B3D, 0xFFFBE3B0, 0xFF7A4510, 0xFFFFF1C9, "solid"),
    SEA_GLASS("Sea Glass", "Frosted bottle glass tumbled by surf. Soft, milky aqua with a cool blue gleam.", 0xFF8FC9B9, 0xFFE3F4EE, 0xFF2F6B5E, 0xFFBFE8FF, "solid"),
    ABALONE("Abalone", "The inside of an abalone shell: teal that flashes violet as it turns.", 0xFF4FA3A5, 0xFFD6F0EC, 0xFF24515E, 0xFFB9A2F0, "solid"),
    MOONSTONE("Moonstone", "Milky grey-white stone with a floating blue glow called adularescence.", 0xFFC9CFDC, 0xFFF6F7FA, 0xFF6B7286, 0xFF8FB6FF, "solid"),
    JEWEL_BEETLE("Jewel Beetle", "A metallic green beetle's shell, shading to gold where the light hits.", 0xFF3E9E5A, 0xFFCDEB9C, 0xFF1D4A2A, 0xFFF0C94A, "solid"),
    ROSE_QUARTZ("Rose Quartz", "Cloudy pink quartz with a faint peach shimmer. The softest of the ten.", 0xFFE7A9B4, 0xFFFBE6EA, 0xFF9A5A66, 0xFFFFD9C4, "solid"),
    LABRADORITE("Labradorite", "Plain grey stone until it tilts, then a sudden peacock-blue flash.", 0xFF6E7A80, 0xFFC9D2D6, 0xFF2F363A, 0xFF48D1C8, "solid"),
    OPAL("Opal", "White with rainbow fire moving under the surface.", 0xFFE6E1F0, 0xFFFFFFFF, 0xFF9B93B8, null, "rainbow"),
    BISMUTH("Bismuth", "A metal crystal that grows its own oil-slick colors: pink, gold and teal.", 0xFF8C7FB5, 0xFFE6DDF7, 0xFF3B3366, null, "bismuth"),
    MOSS_AGATE("Moss Agate", "Pale green stone with moss-like threads inside and a sunlit, buttery gleam.", 0xFF8DA772, 0xFFE6EFD6, 0xFF3F5230, 0xFFF3E3A0, "solid");

    companion object {
        fun byName(name: String?) = entries.firstOrNull { it.name == name } ?: AMBER
    }
}
