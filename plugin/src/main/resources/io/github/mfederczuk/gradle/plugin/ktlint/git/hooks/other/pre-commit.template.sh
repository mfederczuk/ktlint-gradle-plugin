#!/bin/sh
# -*- sh -*-
# vim: syntax=sh
# code: language=shellscript

# Copyright (c) 2023 Michael Federczuk
# SPDX-License-Identifier: MPL-2.0 AND Apache-2.0

# <https://github.com/pinterest/ktlint> custom pre-commit hook
# Generated at //GENERATED_DATETIME::comment_text//
# DO NOT EDIT!

#region preamble

case "$-" in
	(*'i'*)
		\command printf 'script was called interactively\n' >&2
		return 124
		;;
esac

set -o errexit
set -o nounset

# enabling POSIX-compliant behavior for GNU programs
export POSIXLY_CORRECT=yes POSIX_ME_HARDER=yes

if [ "${0#/}" = "$0" ]; then
	argv0="$0"
else
	argv0="$(basename -- "$0" && printf x)"
	argv0="${argv0%"$(printf '\nx')"}"
fi
readonly argv0

if [ $# -gt 0 ]; then
	printf '%s: too many arguments: %i\n' "$argv0" $# >&2
	exit 4
fi

#endregion

git() {
	command git -c diff.noprefix=false --no-pager "$@"
}

git_apply() {
	git apply --ignore-whitespace --whitespace=nowarn --allow-empty "$@"
}

is_absolute_pathname() {
	test "${1#/}" != "$1"
}

mkdirp_parent() {
	set -- "$(dirname -- "$1" && printf x)"
	set -- "${1%"$(printf '\nx')"}"

	mkdir -p -- "$1"
}

is_maybe_using_intellij_idea_terminal() {
	# if git is executed via IntelliJ IDEA, then the environment variable $TERM is set to the value "dumb"
	test "${TERM-}" = 'dumb'
}

is_stdin_color_supported() {
	if [ -n "${NO_COLOR-}" ] || [ ! -t 1 ]; then
		return 32
	fi

	case "${TERM-}" in
		('xterm-color'|*'-256color'|'xterm-kitty')
			return 0
			;;
	esac

	if command -v tput > '/dev/null' && tput setaf 1 1> '/dev/null' 2>&1; then
		return 0
	fi

	return 32
}

#region bare repository check

is_bare_repository="$(git rev-parse --is-bare-repository)"

if [ "$is_bare_repository" = 'true' ]; then
	printf '%s: repository is bare\n' "$argv0" >&2
	exit 48
fi

unset -v is_bare_repository

#endregion

#region setting up temporary directory

base_tmp_dir_pathname="${TMPDIR:-"${TMP:-"${TEMP:-"${TEMPDIR:-"${TMP_DIR:-"${TEMP_DIR:-"/tmp"}"}"}"}"}"}"
base_tmp_dir_pathname="${base_tmp_dir_pathname%"/"}"

if ! is_absolute_pathname "$base_tmp_dir_pathname"; then
	cwd="$(pwd -L && printf x)"
	cwd="${cwd%"$(printf '\nx')"}"

	base_tmp_dir_pathname="$cwd/$base_tmp_dir_pathname"

	unset -v cwd
fi

readonly base_tmp_dir_pathname


process_tmp_dir_pathname="$base_tmp_dir_pathname/ktlint-gradle-plugin-$$"
readonly process_tmp_dir_pathname

remove_process_tmp_dir() {
	# shellcheck disable=2317
	rm -rf -- "$process_tmp_dir_pathname"
}

trap remove_process_tmp_dir EXIT
trap 'trap - EXIT; remove_process_tmp_dir' INT QUIT TERM

#endregion

#region generating unstaged changes patch

unstaged_changes_patch_file_pathname="$process_tmp_dir_pathname/unstaged-changes.patch"
readonly unstaged_changes_patch_file_pathname

mkdirp_parent "$unstaged_changes_patch_file_pathname"

git diff --patch --raw -z --color=never --full-index --binary \
         --output="$unstaged_changes_patch_file_pathname"

#endregion

#region listing staged kotlin files

staged_kotlin_filename_list_file_pathname="$process_tmp_dir_pathname/staged-kotlin-files"
readonly staged_kotlin_filename_list_file_pathname

mkdirp_parent "$staged_kotlin_filename_list_file_pathname"

git diff --name-only -z --color=never --cached --relative \
         --output="$staged_kotlin_filename_list_file_pathname" \
         -- '*.kt' '*.kts'

#endregion

if [ ! -s "$staged_kotlin_filename_list_file_pathname" ]; then
	printf 'No staged Kotlin files; skipping ktlint-check\n' >&2
	exit 0
fi

ktlint_classpath=//KTLINT_CLASSPATH::quoted_string//
readonly ktlint_classpath

ktlint_main_class_name=//KTLINT_MAIN_CLASS_NAME::quoted_string//
readonly ktlint_main_class_name

#region ktlint dry run

if ! java --dry-run -classpath "$ktlint_classpath" "$ktlint_main_class_name"; then
	{
		printf '%s: failed to execute ktlint\n' "$argv0"
		# shellcheck disable=2016
		printf '%s: running the Gradle sync and/or the Gradle task `%s` may fix it\n' "$argv0" //HOOK_INSTALLATION_TASK_NAME::quoted_string//
	} >&2
	exit 48
fi

#endregion

#region running ktlint

ktlint_color_opt_arg=''
if is_stdin_color_supported; then
	ktlint_color_opt_arg='--color'
fi
readonly ktlint_color_opt_arg

ktlint_relative_opt_arg='--relative'
if is_maybe_using_intellij_idea_terminal; then
	# print absolute pathnames if executed via IntelliJ IDEA so that they become clickable
	ktlint_relative_opt_arg=''
fi
readonly ktlint_relative_opt_arg

git_apply --reverse -- "$unstaged_changes_patch_file_pathname"

printf 'Running ktlint (v%s)...\n' //KTLINT_VERSION::quoted_string// >&2

exc=0
java -classpath "$ktlint_classpath" "$ktlint_main_class_name" \
     $ktlint_color_opt_arg \
     $ktlint_relative_opt_arg \
     //KTLINT_CONFIGURED_ARGS::args// \
     --patterns-from-stdin='' < "$staged_kotlin_filename_list_file_pathname" ||
	exc=$?

if [ $exc -eq 0 ]; then
	printf 'Done - no problems found.\n' >&2
fi

git_apply -- "$unstaged_changes_patch_file_pathname"

exit $exc

#endregion
