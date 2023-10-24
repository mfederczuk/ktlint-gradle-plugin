/*
 * SPDX-License-Identifier: CC0-1.0
 */

package io.github.mfederczuk.gradle.plugin.ktlint.utils

import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import javax.annotation.CheckReturnValue

@CheckReturnValue
internal fun <A : Any, B : Any, C : Any, D : Any, E : Any, R : Any> ProviderFactory.zip(
	first: Provider<A>,
	second: Provider<B>,
	third: Provider<C>,
	fourth: Provider<D>,
	fifth: Provider<E>,
	combiner: (A, B, C, D, E) -> R?,
): Provider<R> {
	return this
		.zip(
			this.zip(
				this.zip(first, second, ::Pair),
				this.zip(third, fourth, ::Pair),
				::Pair,
			),
			fifth,
		) { (ab: Pair<A, B>, de: Pair<C, D>), e: E ->
			combiner(ab.first, ab.second, de.first, de.second, e)
		}
}
