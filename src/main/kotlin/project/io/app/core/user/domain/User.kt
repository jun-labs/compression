package project.io.app.core.user.domain

class User(
    private var _id: Long? = null,
    val name: String,
    val marketing: Boolean,
) {

    constructor(
        name: String,
        marketing: Boolean,
    ) : this(
        null, name, marketing
    )

    val id: Long
        get() = _id!!

    fun registerId(id: Long) {
        this._id = id
    }
}
