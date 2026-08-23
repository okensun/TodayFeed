plugins { id("todayfeed.jvm") }

dependencies {
    // The feed is a paged stream, so the contract has to name PagingData. paging-common is a
    // plain JVM artifact, so this module still has no Android on its classpath.
    api(libs.paging.common)
}
