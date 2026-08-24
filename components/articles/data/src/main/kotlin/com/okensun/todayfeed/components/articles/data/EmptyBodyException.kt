package com.okensun.todayfeed.components.articles.data

import java.io.IOException

/**
 * A response the server called a success but which carried nothing. Reporting it as an
 * `HttpException` would surface "HTTP 204" to the reader, which says nothing useful.
 */
internal class EmptyBodyException(
    code: Int,
) : IOException("The source answered $code with no articles in it.")
