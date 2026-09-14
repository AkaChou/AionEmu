#!/usr/bin/env perl

use strict;
use warnings;

for my $path (@ARGV) {
	open my $input, '<', $path or die "cannot read $path: $!\n";
	my @lines = <$input>;
	close $input or die "cannot close $path: $!\n";

	my $inside_misindented_transition = 0;
	my @output;
	for my $line (@lines) {
		if (!$inside_misindented_transition && $line =~ /^      <transition\b/) {
			$line =~ s/^      /    /;
			$inside_misindented_transition = 1 unless $line =~ /\/>\s*$/;
			push @output, $line;
			next;
		}

		if ($inside_misindented_transition) {
			if ($line =~ /^    <\/transition>\s*$/) {
				$inside_misindented_transition = 0;
				push @output, $line;
				next;
			}
			$line =~ s/^(?=\S)/  / unless $line =~ /^\s*$/;
			push @output, $line;
			next;
		}

		$line =~ s/^<\/transitions>\s*$/  <\/transitions>\n/;
		push @output, $line;
	}

	open my $output, '>', $path or die "cannot write $path: $!\n";
	print {$output} @output;
	close $output or die "cannot close $path: $!\n";
}
