We say that the action band says welcome when (
	There exists $p in $(#action-band) such that (
		$p's text matches "^Welcome.*"
	)
).

We say that we click on create cart when (
	There exists $button in $(.button-create-cart) such that (
		$button's event is "click"
	)
).

We say that the login is successful when (
	the action band says welcome
).

"""
  @name Cart already created
  @description It should be impossible to create a cart when one has already been created
  @severity Warning
"""

The next time ( the login is successful ) Then (
	The next time ( we click on create cart ) Then (
		Always (
			Not ( we click on create cart )
		)
	)
).