package app.coinverse.utils

import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule

//FIXME: Filed StackOverflow issue: https://stackoverflow.com/questions/51791067/glide-unable-to-build-appglidemodule-compilesdkversion-28.
@GlideModule
class GlideModule : AppGlideModule()
