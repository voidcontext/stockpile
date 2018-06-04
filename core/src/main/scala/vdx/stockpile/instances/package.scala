package vdx.stockpile

import vdx.stockpile.cardlist.CardListInstances

package object instances {
  // scalastyle:off
  object eq extends EqInstances
  object cardlist extends CardListInstances
  // scalastyle:on
}
