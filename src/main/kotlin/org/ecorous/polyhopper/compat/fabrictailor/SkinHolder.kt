package org.ecorous.polyhopper.compat.fabrictailor

object SkinHolder {
    private val skins: MutableMap<String, String?> = mutableMapOf()

    fun setDefaultSkin(uuid: String, vanillaSkin: String?) {
        skins[uuid] = vanillaSkin
    }

    fun getDefaultSkin(uuid: String) : String? {
        return skins[uuid]
    }
}
