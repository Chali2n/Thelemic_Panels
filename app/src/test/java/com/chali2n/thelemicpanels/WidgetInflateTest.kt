package com.chali2n.thelemicpanels

import android.widget.FrameLayout
import android.widget.TextView
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Every panel must survive being inflated and applied the way a launcher does it.
 *
 * A RemoteViews action aimed at an id missing from that panel's layout, or an
 * unsupported view, throws here; on the device the same throw shows up as the
 * launcher's "Couldn't add widget" error card.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetInflateTest {

    private fun livePanel(kind: PanelKind): Panel = when (kind) {
        PanelKind.DATE -> Panel(
            kind = kind,
            summary = listOf(Seg.strong("Solis"), Seg.muted(" in 16° Scorpii")),
            cta = Thelemic.CTA_LABEL,
            ctaUrl = Thelemic.CTA_URL,
        )
        PanelKind.LUNAR -> Lunar.panel(System.currentTimeMillis())
        PanelKind.RESH -> Resh.panel(System.currentTimeMillis())
        PanelKind.HOURS -> Hours.panel(System.currentTimeMillis())
    }

    @Test
    fun everyPanelAppliesTheWayALauncherAppliesIt() {
        val context = RuntimeEnvironment.getApplication()
        PanelKind.entries.forEach { kind ->
            // The placeholder is what the launcher sees on the very first bind,
            // before any refresh has run, so it has to inflate too.
            listOf("placeholder" to WidgetRender.placeholder(kind), "live" to livePanel(kind))
                .forEach { (label, panel) ->
                    val views = WidgetRender.build(context, kind, panel, 1)
                    val applied = views.apply(context, FrameLayout(context))
                    assertNotNull("$kind ($label) produced no view", applied)

                    val summary = applied.findViewById<TextView>(R.id.panel_summary)
                    assertTrue("$kind ($label) has no summary text", !summary.text.isNullOrEmpty())
                }
        }
    }
}
