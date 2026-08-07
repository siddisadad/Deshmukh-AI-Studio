import { FormControl, FormHelperText, InputLabel, MenuItem, Select, type SelectChangeEvent } from '@mui/material';
import type { Requirement } from '../../requirements/api/requirementsApi';

interface Props {
  requirements: Requirement[];
  value: string;
  onChange: (requirementId: string) => void;
  label?: string;
  testId?: string;
}

/** Empty string means unlinked; option values are requirement ids. */
export function RequirementSelect({
  requirements,
  value,
  onChange,
  label = 'Requirement',
  testId = 'requirement-select',
}: Props) {
  function handleChange(event: SelectChangeEvent<string>) {
    onChange(event.target.value);
  }

  return (
    <FormControl fullWidth>
      <InputLabel id={`${testId}-label`}>{label}</InputLabel>
      <Select
        labelId={`${testId}-label`}
        label={label}
        value={value}
        onChange={handleChange}
        data-testid={testId}
      >
        <MenuItem value="" data-testid={`${testId}-none`}>
          None
        </MenuItem>
        {requirements.map((req) => (
          <MenuItem key={req.id} value={req.id} data-testid={`${testId}-option-${req.id}`}>
            {req.title}
          </MenuItem>
        ))}
      </Select>
      {requirements.length === 0 && (
        <FormHelperText data-testid={`${testId}-empty`}>
          No requirements yet — create one under Requirements, then link it here
        </FormHelperText>
      )}
    </FormControl>
  );
}
