package com.andb.apps.biblio.ui.home

import android.content.Context
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser

class ReadiumUtils(context: Context) {
    val httpClient =
        DefaultHttpClient()

    val assetRetriever =
        AssetRetriever(context.contentResolver, httpClient)

    val publicationParser =
        DefaultPublicationParser(context, httpClient, assetRetriever, null)

    val publicationOpener =
        PublicationOpener(publicationParser)

}