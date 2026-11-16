package app.badcat.client.runtime

import android.content.Context
import app.badcat.client.FileBookSourceCatalog
import app.badcat.client.FileReplaceRuleCatalog
import app.badcat.engine.BookTextReplacement
import app.badcat.engine.CatalogLegadoBookTextReplacement
import app.badcat.engine.BookSourceSearchUrlResolver
import app.badcat.engine.OkHttpSourceRequestClient
import app.badcat.engine.ReadingEngine
import app.badcat.engine.SourceBookDetailService
import app.badcat.engine.SourceChapterContentService
import app.badcat.engine.SourceExploreService
import app.badcat.engine.SourceReadingEngine
import app.badcat.engine.SourceSearchService
import app.badcat.engine.SourceTocService
import app.badcat.engine.SourceRequestClient
import app.badcat.engine.SourceScriptRuntime

/**
 * Builds the Android host's shared, multi-source reading-engine graph.
 *
 * All four source services share one gateway whose embedded evaluator is
 * selected from the BookSource supplied to each request. The source catalog
 * remains app-private and durable. Purification is injected separately and
 * applies imported Legado title/content rules with bounded regex before the optional host hook.
 */
class BadcatReadingEngineFactory(
    private val context: Context,
    private val requestClient: SourceRequestClient? = null,
    private val scriptRuntime: SourceScriptRuntime = RhinoSourceScriptRuntime(),
    private val bookTextReplacement: BookTextReplacement = BookTextReplacement { text -> text },
) {
    private val catalog = FileBookSourceCatalog(context.applicationContext)
    private val replaceRuleCatalog = FileReplaceRuleCatalog(context.applicationContext)

    fun create(): ReadingEngine {
        val durableRequestClient = requestClient ?: OkHttpSourceRequestClient(
            cookieJarProvider = { sourceId ->
                SharedPreferencesSourceCookieJar(context.applicationContext, sourceId)
            },
        )
        val gateway = BadcatSourceHttpGatewayFactory(
            client = durableRequestClient,
            scriptRuntime = scriptRuntime,
        ).create()

        return SourceReadingEngine(
            catalog = catalog,
            searchUrlResolver = BookSourceSearchUrlResolver(),
            searchService = SourceSearchService(gateway, scriptRuntime = scriptRuntime),
            exploreService = SourceExploreService(gateway, scriptRuntime = scriptRuntime),
            detailService = SourceBookDetailService(gateway, scriptRuntime = scriptRuntime),
            tocService = SourceTocService(gateway, scriptRuntime = scriptRuntime),
            contentService = SourceChapterContentService(
                http = gateway,
                textReplacement = bookTextReplacement,
                scopedTextReplacement = CatalogLegadoBookTextReplacement(
                    catalog = replaceRuleCatalog,
                    fallback = bookTextReplacement,
                    jsEvaluator = RhinoLegadoJsReplacementEvaluator(),
                ),
                cache = FileChapterContentCache(context.applicationContext),
                scriptRuntime = scriptRuntime,
            ),
        )
    }

    /** Exposes the same durable catalog for import/export and enabled-state UI. */
    fun sourceCatalog(): FileBookSourceCatalog = catalog

    /** Exposes durable Legado replacement-rule import/export storage. */
    fun replaceRuleCatalog(): FileReplaceRuleCatalog = replaceRuleCatalog
}
