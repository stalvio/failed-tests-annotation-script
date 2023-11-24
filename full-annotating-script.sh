#!/bin/bash

temp_file="temp.txt"

# Loop through each HTML file in the folder
for file in *.html; do
	
    # Get the filename without extension and change .html to .groove
    file_name="${file%.html}.groovy"

    # Read the file until 'special text to stop' is encountered
    while IFS='' read -r line || [[ -n "$line" ]]; do
	
        if [[ $line == *"special text to stop"* ]]; then
            break
        fi

        # Extract string between 'special start' and 'special finish'
        if [[ $line == *"special start"* ]]; then
            extracted_string=$(echo "$line" | sed -n 's/.*special start\(.*\)special finish.*/\1/p')
            echo "$extracted_string" >>temp_file.txt
        fi
    done < "$file"

    # Check if temp file exists and if the groove file exists
    if [ -f "temp_file.txt" ] && [ -f "$file_name" ]; then
						
		while IFS= read -r groovy_line || [ -n "$groovy_line" ]; do
			echo "${groovy_line}"
			while IFS='' read -r extracted_line || [[ -n "$extracted_line" ]]; do
				echo "line_num: ${line_num}"
				if [[ "$groovy_line" == *"${extracted_line}"* ]]; then
					echo "I found the match!!"
					rev_line_num=${line_num}
					echo "rev_line_num: ${rev_line_num}"
					while IIFS= read -r rev_line || [ -n "$rev_line" ]; do
						
						echo "Reversing"
						echo "${rev_line_num}"
						echo "${rev_line}"
						if [[ "${rev_line%"$'\n'"}" == "def "* ]]; then
							echo "I found def!!!"
							break
						fi
						trimmed_line=$(echo "$rev_line" | sed -e 's/^[ \t]*//;s/[ \t]*$//')
						if [ -z "${trimmed_line}" ]; then
							echo "I found an empty line!!!"
							sed -i "${rev_line_num}s/^/@IgnoreRest/" "$temp_file"
							break
						fi
						((rev_line_num--))
					done < <(tac "$temp_file")
				fi				
			done <temp_file.txt
			echo "$groovy_line" >> "$temp_file"
			((line_num++))
		done < "$file_name"
		mv "$temp_file" "$file_name"	
			
    fi

    # Clear temp file for next iteration
    >temp_file.txt
done
