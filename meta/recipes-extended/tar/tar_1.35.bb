SUMMARY = "GNU file archiving program"
DESCRIPTION = "GNU tar saves many files together into a single tape \
or disk archive, and can restore individual files from the archive."
HOMEPAGE = "http://www.gnu.org/software/tar/"
SECTION = "base"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=1ebbd3e34237af26da5dc08a4e440464"

SRC_URI = "${GNU_MIRROR}/tar/tar-${PV}.tar.bz2"

SRC_URI[sha256sum] = "7edb8886a3dc69420a1446e1e2d061922b642f1cf632d2cd0f9ee7e690775985"

inherit autotools gettext texinfo

PACKAGECONFIG ??= ""
PACKAGECONFIG:append:class-target = " ${@bb.utils.filter('DISTRO_FEATURES', 'acl', d)}"

PACKAGECONFIG[acl] = "--with-posix-acls,--without-posix-acls,acl"
PACKAGECONFIG[selinux] = "--with-selinux,--without-selinux,libselinux"

EXTRA_OECONF += "DEFAULT_RMT_DIR=${sbindir}"

CACHED_CONFIGUREVARS += "tar_cv_path_RSH=no"

do_install () {
    autotools_do_install
    ln -s tar ${D}${bindir}/gtar
}

do_install:append:class-target() {
    if [ "${base_bindir}" != "${bindir}" ]; then
        install -d ${D}${base_bindir}
        mv ${D}${bindir}/tar ${D}${base_bindir}/tar
        mv ${D}${bindir}/gtar ${D}${base_bindir}/gtar
        rmdir ${D}${bindir}/
    fi
}

# add for ptest support
SRC_URI += " \
    file://run-ptest \
    file://0001-tests-fix-TESTSUITE_AT.patch \
    file://0002-tests-check-for-recently-fixed-bug.patch \
    file://0003-Exclude-VCS-directory-with-writing-from-an-archive.patch \
"

inherit ptest

do_compile_ptest() {
    oe_runmake -C ${B}/gnu/ check
    oe_runmake -C ${B}/lib/ check
    oe_runmake -C ${B}/rmt/ check
    oe_runmake -C ${B}/src/ check
    rm -rf ${S}/tests/testsuite
    oe_runmake -C ${B}/tests/ testsuite
    oe_runmake -C ${B}/tests/ genfile checkseekhole ckmtime
}

do_install_ptest() {
    install -d ${D}${PTEST_PATH}/tests/
    install --mode=755 ${B}/tests/atconfig ${D}${PTEST_PATH}/tests/
    sed -i "/abs_/d" ${D}${PTEST_PATH}/tests/atconfig
    echo "abs_builddir=${PTEST_PATH}/tests/" >> ${D}${PTEST_PATH}/tests/atconfig
    install --mode=755 ${B}/tests/atlocal ${D}${PTEST_PATH}/tests/
    sed -i "/PATH=/d" ${D}${PTEST_PATH}/tests/atlocal
    install --mode=755 ${B}/tests/genfile ${D}${PTEST_PATH}/tests/
    install --mode=755 ${B}/tests/checkseekhole ${D}${PTEST_PATH}/tests/
    install --mode=755 ${B}/tests/ckmtime ${D}${PTEST_PATH}/tests/
    install --mode=755 ${S}/tests/testsuite ${D}${PTEST_PATH}/tests/
    sed -i "s#@PTEST_PATH@#${PTEST_PATH}#g" ${D}${PTEST_PATH}/run-ptest
}

PACKAGES =+ "${PN}-rmt"

FILES:${PN}-rmt = "${sbindir}/rmt*"

inherit update-alternatives

ALTERNATIVE_PRIORITY = "100"

ALTERNATIVE:${PN} = "tar"
ALTERNATIVE:${PN}-rmt = "rmt"
ALTERNATIVE:${PN}:class-nativesdk = ""
ALTERNATIVE:${PN}-rmt:class-nativesdk = ""

ALTERNATIVE_LINK_NAME[tar] = "${base_bindir}/tar"
ALTERNATIVE_LINK_NAME[rmt] = "${sbindir}/rmt"

PROVIDES:append:class-native = " tar-replacement-native"
NATIVE_PACKAGE_PATH_SUFFIX = "/${PN}"

BBCLASSEXTEND = "native nativesdk"

# Avoid false positives from CVEs in node-tar package
# For example CVE-2021-{32803,32804,37701,37712,37713}
CVE_PRODUCT = "gnu:tar"
