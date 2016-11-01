# We define a predicate using the construct
# "We say that <arguments> is/are <predicate name> when".

We say that $x and $y are left-aligned when (
  $x's left equals $y's left
).

We say that $x and $y are top-aligned when (
  $x's top equals $y's top
).

# We then express the fact that all menu items are either
# left- or top-aligned.

"""
  @name Menus aligned
  @description All list items should either be left- or top-aligned.
  @severity Warning
"""
For each $z in $(.menu li) (
  For each $t in $(.menu li) (
    ($z and $t are left-aligned)
    Or
    ($z and $t are top-aligned)
  )
).
